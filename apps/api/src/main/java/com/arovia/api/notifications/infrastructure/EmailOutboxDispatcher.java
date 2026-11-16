package com.arovia.api.notifications.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import com.arovia.api.notifications.domain.ClaimedOutboxMessage;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailOutboxDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(EmailOutboxDispatcher.class);

    private static final String VERIFICATION_EVENT_TYPE =
            "USER_EMAIL_VERIFICATION_REQUESTED";

    private static final String PASSWORD_RESET_EVENT_TYPE =
            "USER_PASSWORD_RESET_REQUESTED";

    private static final int MAX_ATTEMPTS = 8;
    private static final long BASE_RETRY_SECONDS = 60;
    private static final long MAX_RETRY_SECONDS = 3600;
    private static final long MAX_JITTER_SECONDS = 30;

    private final EmailOutboxStore outboxStore;
    private final OutboxPayloadCipher payloadCipher;
    private final JavaMailSender mailSender;
    private final Clock clock;

    private final String webBaseUrl;
    private final String fromAddress;

    private final Duration verificationTokenTtl;
    private final Duration passwordResetTokenTtl;

    private final int batchSize;

    public EmailOutboxDispatcher(
            EmailOutboxStore outboxStore,
            OutboxPayloadCipher payloadCipher,
            JavaMailSender mailSender,
            Clock applicationClock,
            @Value("${arovia.app.web-base-url}")
            String webBaseUrl,
            @Value("${arovia.mail.from}")
            String fromAddress,
            @Value("${arovia.identity.email-verification-ttl:PT24H}")
            Duration verificationTokenTtl,
            @Value("${arovia.identity.password-reset-ttl:PT30M}")
            Duration passwordResetTokenTtl,
            @Value("${arovia.outbox.batch-size:10}")
            int batchSize
    ) {
        this.outboxStore = outboxStore;
        this.payloadCipher = payloadCipher;
        this.mailSender = mailSender;
        this.clock = applicationClock;
        this.webBaseUrl = removeTrailingSlashes(webBaseUrl);
        this.fromAddress = fromAddress;
        this.verificationTokenTtl = verificationTokenTtl;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(
            fixedDelayString = "${arovia.outbox.poll-interval-ms:5000}"
    )
    public void dispatchPendingEmails() {
        for (int index = 0; index < batchSize; index++) {
            var claimedMessage = outboxStore.claimNext();

            if (claimedMessage.isEmpty()) {
                return;
            }

            process(claimedMessage.get());
        }
    }

    private void process(ClaimedOutboxMessage message) {

        try {
            if (VERIFICATION_EVENT_TYPE.equals(message.eventType())) {
                sendVerificationEmail(
                        decryptVerificationPayload(message)
                );

            } else if (
                    PASSWORD_RESET_EVENT_TYPE.equals(message.eventType())
            ) {
                sendPasswordResetEmail(
                        decryptPasswordResetPayload(message)
                );

            } else {
                outboxStore.markDeadLetter(
                        message.id(),
                        "UNSUPPORTED_EVENT_TYPE"
                );

                log.warn(
                        "Unsupported email event moved to dead letter: {}",
                        message.id()
                );

                return;
            }

        } catch (MailException exception) {
            handleDeliveryFailure(message, exception);
            return;

        } catch (RuntimeException exception) {
            outboxStore.markDeadLetter(
                    message.id(),
                    "PAYLOAD_PROCESSING_FAILED"
            );

            log.error(
                    "Email outbox message processing failed. "
                            + "Message moved to dead letter: {}",
                    message.id()
            );

            return;
        }

        outboxStore.markSent(message.id());

        log.info(
                "Email outbox message delivered successfully: {}",
                message.id()
        );
    }

    private EmailVerificationEmailPayload decryptVerificationPayload(
            ClaimedOutboxMessage message
    ) {
        EncryptedOutboxPayload encryptedPayload =
                toEncryptedPayload(message);

        return payloadCipher.decrypt(
                message.id(),
                message.eventType(),
                encryptedPayload
        );
    }

    private PasswordResetEmailPayload decryptPasswordResetPayload(
            ClaimedOutboxMessage message
    ) {
        EncryptedOutboxPayload encryptedPayload =
                toEncryptedPayload(message);

        return payloadCipher.decryptPasswordReset(
                message.id(),
                message.eventType(),
                encryptedPayload
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

    private void handleDeliveryFailure(
            ClaimedOutboxMessage message,
            MailException exception
    ) {
        String errorCode = "SMTP_DELIVERY_FAILED";

        if (message.attemptCount() >= MAX_ATTEMPTS) {
            outboxStore.markDeadLetter(
                    message.id(),
                    errorCode
            );

            log.error(
                    "Email delivery attempts exhausted. "
                            + "Message moved to dead letter: {}",
                    message.id()
            );

            return;
        }

        Instant nextAttemptAt = clock.instant().plusSeconds(
                calculateRetryDelaySeconds(message.attemptCount())
        );

        outboxStore.scheduleRetry(
                message.id(),
                nextAttemptAt,
                errorCode
        );

        log.warn(
                "Email delivery failed; retry scheduled. "
                        + "outboxId={}, attempt={}, errorType={}",
                message.id(),
                message.attemptCount(),
                exception.getClass().getSimpleName()
        );
    }

    private long calculateRetryDelaySeconds(int attemptCount) {
        int exponent = Math.min(
                Math.max(attemptCount - 1, 0),
                10
        );

        long exponentialDelay =
                BASE_RETRY_SECONDS * (1L << exponent);

        long boundedDelay = Math.min(
                exponentialDelay,
                MAX_RETRY_SECONDS
        );

        long jitter = ThreadLocalRandom.current().nextLong(
                MAX_JITTER_SECONDS + 1
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