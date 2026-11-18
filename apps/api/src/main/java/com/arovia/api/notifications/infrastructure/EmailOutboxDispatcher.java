package com.arovia.api.notifications.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import com.arovia.api.configuration.AroviaProperties;
import com.arovia.api.configuration.IdentityWorkflowProperties;
import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.ClaimedOutboxMessage;
import com.arovia.api.notifications.domain.EmailOutboxErrorCode;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailOutboxDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(EmailOutboxDispatcher.class);

    private final EmailOutboxStore outboxStore;
    private final OutboxPayloadCipher payloadCipher;
    private final JavaMailSender mailSender;
    private final EmailOutboxObservability observability;
    private final Clock clock;

    private final String webBaseUrl;
    private final String fromAddress;

    private final Duration verificationTokenTtl;
    private final Duration passwordResetTokenTtl;

    private final int batchSize;
    private final AroviaProperties.Outbox.Retry retryPolicy;

    public EmailOutboxDispatcher(
            EmailOutboxStore outboxStore,
            OutboxPayloadCipher payloadCipher,
            JavaMailSender mailSender,
            EmailOutboxObservability observability,
            Clock applicationClock,
            AroviaProperties aroviaProperties,
            IdentityWorkflowProperties workflowProperties
    ) {
        this.outboxStore = outboxStore;
        this.payloadCipher = payloadCipher;
        this.mailSender = mailSender;
        this.observability = observability;
        this.clock = applicationClock;

        this.webBaseUrl = removeTrailingSlashes(
                aroviaProperties.getApp().getWebBaseUrl()
        );

        this.fromAddress = aroviaProperties.getMail().getFrom();

        this.verificationTokenTtl =
                workflowProperties.getEmailVerificationTtl();

        this.passwordResetTokenTtl =
                workflowProperties.getPasswordResetTtl();

        this.batchSize = aroviaProperties.getOutbox().getBatchSize();
        this.retryPolicy = aroviaProperties.getOutbox().getRetry();
    }

    @Scheduled(
            fixedDelayString = "${arovia.outbox.poll-interval-ms:5000}"
    )
    public void dispatchPendingEmails() {
        for (int index = 0; index < batchSize; index++) {
            var claimedMessage = claimNextMessage();

            if (claimedMessage.isEmpty()) {
                return;
            }

            process(claimedMessage.get());
        }
    }

    private java.util.Optional<ClaimedOutboxMessage> claimNextMessage() {
        try {
            return outboxStore.claimNext();

        } catch (RuntimeException exception) {
            observability.recordPollFailure();

            log.atError()
                    .addKeyValue(
                            "error_type",
                            exception.getClass().getSimpleName()
                    )
                    .log("Unable to claim an email outbox message.");

            throw exception;
        }
    }

    private void process(ClaimedOutboxMessage message) {
        long startedAtNanos = System.nanoTime();

        /*
         * If processing throws unexpectedly, the failure is classified
         * as internal_error. The processing duration is still recorded.
         */
        String outcome = EmailOutboxObservability.OUTCOME_INTERNAL_ERROR;

        try {
            outcome = processMessage(message);

        } finally {
            Duration processingDuration = Duration.ofNanos(
                    Math.max(0L, System.nanoTime() - startedAtNanos)
            );

            observability.recordDispatch(
                    message.eventType(),
                    outcome,
                    processingDuration
            );
        }
    }

    private String processMessage(ClaimedOutboxMessage message) {

        /*
         * Unsupported events are rejected before attempting decryption.
         */
        if (!isSupportedEventType(message.eventType())) {
            outboxStore.markDeadLetter(
                    message.id(),
                    EmailOutboxErrorCode.UNSUPPORTED_EVENT_TYPE.value()
            );

            log.atWarn()
                    .addKeyValue("outbox_id", message.id())
                    .addKeyValue("event_type", message.eventType())
                    .addKeyValue(
                            "outcome",
                            EmailOutboxObservability
                                    .OUTCOME_UNSUPPORTED_EVENT_TYPE
                    )
                    .log("Unsupported email event moved to dead letter.");

            return EmailOutboxObservability.OUTCOME_UNSUPPORTED_EVENT_TYPE;
        }

        try {
            if (EmailNotificationType.EMAIL_VERIFICATION.eventType()
                    .equals(message.eventType())) {

                sendVerificationEmail(
                        decryptVerificationPayload(message)
                );

            } else {
                sendPasswordResetEmail(
                        decryptPasswordResetPayload(message)
                );
            }

        } catch (MailException exception) {
            return handleDeliveryFailure(message, exception);

        } catch (OutboxEncryptionKeyUnavailableException exception) {
            outboxStore.scheduleRetryWithoutConsumingAttempt(
                    message.id(),
                    clock.instant().plusSeconds(
                            retryPolicy.getMaxRetryDelaySeconds()
                    ),
                    EmailOutboxErrorCode.ENCRYPTION_KEY_UNAVAILABLE.value()
            );

            log.atError()
                    .addKeyValue("outbox_id", message.id())
                    .addKeyValue(
                            "key_version",
                            message.encryptionKeyVersion()
                    )
                    .addKeyValue(
                            "outcome",
                            EmailOutboxObservability
                                    .OUTCOME_ENCRYPTION_KEY_UNAVAILABLE
                    )
                    .log(
                            "Email outbox message is waiting for its "
                                    + "encryption key configuration."
                    );

            return EmailOutboxObservability
                    .OUTCOME_ENCRYPTION_KEY_UNAVAILABLE;

        } catch (RuntimeException exception) {
            outboxStore.markDeadLetter(
                    message.id(),
                    EmailOutboxErrorCode.PAYLOAD_PROCESSING_FAILED.value()
            );

            log.atError()
                    .addKeyValue("outbox_id", message.id())
                    .addKeyValue("event_type", message.eventType())
                    .addKeyValue(
                            "error_type",
                            exception.getClass().getSimpleName()
                    )
                    .addKeyValue(
                            "outcome",
                            EmailOutboxObservability
                                    .OUTCOME_PAYLOAD_PROCESSING_FAILED
                    )
                    .log(
                            "Email outbox payload processing failed. "
                                    + "Message moved to dead letter."
                    );

            return EmailOutboxObservability
                    .OUTCOME_PAYLOAD_PROCESSING_FAILED;
        }

        /*
         * Mark SENT only after the mail provider accepts the message.
         */
        outboxStore.markSent(message.id());

        log.atInfo()
                .addKeyValue("outbox_id", message.id())
                .addKeyValue("event_type", message.eventType())
                .addKeyValue(
                        "outcome",
                        EmailOutboxObservability.OUTCOME_SENT
                )
                .log("Email outbox message delivered successfully.");

        return EmailOutboxObservability.OUTCOME_SENT;
    }

    private boolean isSupportedEventType(String eventType) {
        return EmailNotificationType.EMAIL_VERIFICATION.eventType()
                .equals(eventType)
                || EmailNotificationType.PASSWORD_RESET.eventType()
                .equals(eventType);
    }

    private EmailVerificationEmailPayload decryptVerificationPayload(
            ClaimedOutboxMessage message
    ) {
        return payloadCipher.decrypt(
                message.id(),
                message.eventType(),
                toEncryptedPayload(message)
        );
    }

    private PasswordResetEmailPayload decryptPasswordResetPayload(
            ClaimedOutboxMessage message
    ) {
        return payloadCipher.decryptPasswordReset(
                message.id(),
                message.eventType(),
                toEncryptedPayload(message)
        );
    }

    private EncryptedOutboxPayload toEncryptedPayload(
            ClaimedOutboxMessage message
    ) {
        return new EncryptedOutboxPayload(
                message.ciphertext(),
                message.nonce(),
                message.encryptionKeyVersion()
        );
    }

    private void sendVerificationEmail(
            EmailVerificationEmailPayload payload
    ) {
        String verificationUrl = webBaseUrl
                + "/verify-email?token="
                + payload.verificationToken();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(payload.recipientEmail());
        message.setSubject("Verify your Arovia account");

        message.setText("""
                Hello %s,

                Thank you for registering with Arovia.

                Please verify your email address by opening this link:

                %s

                This verification link expires %s.

                If you did not request an Arovia account, you can ignore this email.

                Regards,
                Arovia Team
                """.formatted(
                payload.displayName(),
                verificationUrl,
                describeExpiry(verificationTokenTtl)
        ));

        mailSender.send(message);
    }

    private void sendPasswordResetEmail(
            PasswordResetEmailPayload payload
    ) {
        String resetUrl = webBaseUrl
                + "/reset-password?token="
                + payload.resetToken();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(payload.recipientEmail());
        message.setSubject("Reset your Arovia password");

        message.setText("""
                Hello %s,

                We received a request to reset the password for your Arovia account.

                You can choose a new password using this secure link:

                %s

                This link expires %s and can only be used once.

                If you did not request a password reset, you can ignore this email.
                Your existing password will remain unchanged.

                Regards,
                Arovia Team
                """.formatted(
                payload.displayName(),
                resetUrl,
                describeExpiry(passwordResetTokenTtl)
        ));

        mailSender.send(message);
    }

    private String describeExpiry(Duration duration) {
        long totalMinutes = Math.max(1, duration.toMinutes());

        if (totalMinutes < 60) {
            return "in " + totalMinutes + " minutes";
        }

        long hours = totalMinutes / 60;
        long remainingMinutes = totalMinutes % 60;

        if (remainingMinutes == 0) {
            return "in " + hours + " hours";
        }

        return "in " + hours + " hours and "
                + remainingMinutes + " minutes";
    }

    private String handleDeliveryFailure(
            ClaimedOutboxMessage message,
            MailException exception
    ) {
        EmailOutboxErrorCode errorCode =
                EmailOutboxErrorCode.SMTP_DELIVERY_FAILED;

        if (message.attemptCount() >= retryPolicy.getMaxAttempts()) {
            outboxStore.markDeadLetter(
                    message.id(),
                    errorCode.value()
            );

            log.atError()
                    .addKeyValue("outbox_id", message.id())
                    .addKeyValue("attempt", message.attemptCount())
                    .addKeyValue(
                            "error_type",
                            exception.getClass().getSimpleName()
                    )
                    .addKeyValue(
                            "outcome",
                            EmailOutboxObservability.OUTCOME_SMTP_DEAD_LETTER
                    )
                    .log(
                            "Email delivery attempts exhausted. "
                                    + "Message moved to dead letter."
                    );

            return EmailOutboxObservability.OUTCOME_SMTP_DEAD_LETTER;
        }

        Instant nextAttemptAt = clock.instant().plusSeconds(
                calculateRetryDelaySeconds(message.attemptCount())
        );

        outboxStore.scheduleRetry(
                message.id(),
                nextAttemptAt,
                errorCode.value()
        );

        log.atWarn()
                .addKeyValue("outbox_id", message.id())
                .addKeyValue("attempt", message.attemptCount())
                .addKeyValue(
                        "error_type",
                        exception.getClass().getSimpleName()
                )
                .addKeyValue(
                        "outcome",
                        EmailOutboxObservability.OUTCOME_SMTP_RETRY
                )
                .log("Email delivery failed. Retry scheduled.");

        return EmailOutboxObservability.OUTCOME_SMTP_RETRY;
    }

    private long calculateRetryDelaySeconds(int attemptCount) {
        int exponent = Math.min(
                Math.max(attemptCount - 1, 0),
                10
        );

        long boundedDelay =
                retryPolicy.getBaseRetryDelaySeconds();

        long maximumDelay =
                retryPolicy.getMaxRetryDelaySeconds();

        for (int index = 0; index < exponent; index++) {
            if (boundedDelay >= maximumDelay
                    || boundedDelay > maximumDelay / 2) {
                boundedDelay = maximumDelay;
                break;
            }

            boundedDelay = Math.min(
                    boundedDelay * 2,
                    maximumDelay
            );
        }

        long maximumJitter = retryPolicy.getMaxJitterSeconds();

        long jitter = maximumJitter == 0
                ? 0
                : ThreadLocalRandom.current().nextLong(
                maximumJitter + 1
        );

        return boundedDelay + jitter;
    }

    private static String removeTrailingSlashes(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Arovia web base URL must be configured."
            );
        }

        return value.replaceAll("/+$", "");
    }
}