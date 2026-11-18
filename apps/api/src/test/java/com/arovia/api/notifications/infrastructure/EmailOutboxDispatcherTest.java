package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.configuration.AroviaProperties;
import com.arovia.api.configuration.IdentityWorkflowProperties;
import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.ClaimedOutboxMessage;
import com.arovia.api.notifications.domain.EmailOutboxErrorCode;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailOutboxDispatcherTest {

    private static final String EVENT_TYPE =
            EmailNotificationType.EMAIL_VERIFICATION.eventType();

    @Mock
    private EmailOutboxStore outboxStore;

    @Mock
    private OutboxPayloadCipher payloadCipher;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailOutboxObservability observability;

    @Test
    void sendsVerificationEmailAndMarksOutboxSent() {
        UUID outboxId = UUID.randomUUID();
        ClaimedOutboxMessage claimedMessage = createMessage(outboxId);

        EmailVerificationEmailPayload payload =
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "test-verification-token"
                );

        when(outboxStore.claimNext())
                .thenReturn(Optional.of(claimedMessage));

        when(payloadCipher.decrypt(
                outboxId,
                EVENT_TYPE,
                new EncryptedOutboxPayload(
                        claimedMessage.ciphertext(),
                        claimedMessage.nonce(),
                        claimedMessage.encryptionKeyVersion()
                )
        )).thenReturn(payload);

        EmailOutboxDispatcher dispatcher = createDispatcher();

        dispatcher.dispatchPendingEmails();

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("user@example.com", sentMessage.getTo()[0]);

        assertEquals(
                "Verify your Arovia account",
                sentMessage.getSubject()
        );

        assertTrue(
                sentMessage.getText().contains(
                        "http://localhost:3000/verify-email?token="
                                + "test-verification-token"
                )
        );

        assertTrue(
                sentMessage.getText().contains("24 hours")
        );

        verify(outboxStore).markSent(outboxId);

        verify(observability).recordDispatch(
                eq(EVENT_TYPE),
                eq(EmailOutboxObservability.OUTCOME_SENT),
                any(Duration.class)
        );
    }

    @Test
    void schedulesRetryWhenSmtpDeliveryFails() {
        UUID outboxId = UUID.randomUUID();
        ClaimedOutboxMessage claimedMessage = createMessage(outboxId);

        EmailVerificationEmailPayload payload =
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "test-verification-token"
                );

        when(outboxStore.claimNext())
                .thenReturn(Optional.of(claimedMessage));

        when(payloadCipher.decrypt(
                outboxId,
                EVENT_TYPE,
                new EncryptedOutboxPayload(
                        claimedMessage.ciphertext(),
                        claimedMessage.nonce(),
                        claimedMessage.encryptionKeyVersion()
                )
        )).thenReturn(payload);

        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        EmailOutboxDispatcher dispatcher = createDispatcher();

        dispatcher.dispatchPendingEmails();

        verify(outboxStore).scheduleRetry(
                eq(outboxId),
                any(),
                eq(EmailOutboxErrorCode.SMTP_DELIVERY_FAILED.value())
        );

        verify(observability).recordDispatch(
                eq(EVENT_TYPE),
                eq(EmailOutboxObservability.OUTCOME_SMTP_RETRY),
                any(Duration.class)
        );
    }

    @Test
    void preservesMessageWhenEncryptionKeyVersionIsUnavailable() {
        UUID outboxId = UUID.randomUUID();

        ClaimedOutboxMessage claimedMessage =
                new ClaimedOutboxMessage(
                        outboxId,
                        EVENT_TYPE,
                        new byte[]{1, 2, 3},
                        new byte[12],
                        "retired-v1",
                        99
                );

        when(outboxStore.claimNext())
                .thenReturn(Optional.of(claimedMessage));

        doThrow(
                new OutboxEncryptionKeyUnavailableException("retired-v1")
        ).when(payloadCipher).decrypt(
                eq(outboxId),
                eq(EVENT_TYPE),
                any(EncryptedOutboxPayload.class)
        );

        EmailOutboxDispatcher dispatcher = createDispatcher();

        dispatcher.dispatchPendingEmails();

        verify(outboxStore).scheduleRetryWithoutConsumingAttempt(
                eq(outboxId),
                any(Instant.class),
                eq(EmailOutboxErrorCode.ENCRYPTION_KEY_UNAVAILABLE.value())
        );

        verify(outboxStore, never()).markDeadLetter(
                any(UUID.class),
                anyString()
        );

        verify(outboxStore, never()).markSent(any(UUID.class));

        verify(mailSender, never()).send(
                any(SimpleMailMessage.class)
        );

        verify(observability).recordDispatch(
                eq(EVENT_TYPE),
                eq(
                        EmailOutboxObservability
                                .OUTCOME_ENCRYPTION_KEY_UNAVAILABLE
                ),
                any(Duration.class)
        );
    }

    @Test
    void movesUnsupportedEventToDeadLetter() {
        UUID outboxId = UUID.randomUUID();

        ClaimedOutboxMessage claimedMessage = new ClaimedOutboxMessage(
                outboxId,
                "UNSUPPORTED_EVENT",
                new byte[]{1},
                new byte[12],
                "local-v1",
                1
        );

        when(outboxStore.claimNext())
                .thenReturn(Optional.of(claimedMessage));

        EmailOutboxDispatcher dispatcher = createDispatcher();

        dispatcher.dispatchPendingEmails();

        verify(outboxStore).markDeadLetter(
                outboxId,
                EmailOutboxErrorCode.UNSUPPORTED_EVENT_TYPE.value()
        );

        verify(observability).recordDispatch(
                eq("UNSUPPORTED_EVENT"),
                eq(EmailOutboxObservability.OUTCOME_UNSUPPORTED_EVENT_TYPE),
                any(Duration.class)
        );
    }

    private EmailOutboxDispatcher createDispatcher() {
        AroviaProperties aroviaProperties = new AroviaProperties();

        aroviaProperties.getApp()
                .setWebBaseUrl("http://localhost:3000");

        aroviaProperties.getMail()
                .setFrom("no-reply@arovia.local");

        aroviaProperties.getOutbox()
                .setBatchSize(1);

        IdentityWorkflowProperties workflowProperties =
                new IdentityWorkflowProperties();

        return new EmailOutboxDispatcher(
                outboxStore,
                payloadCipher,
                mailSender,
                observability,
                Clock.systemUTC(),
                aroviaProperties,
                workflowProperties
        );
    }

    private ClaimedOutboxMessage createMessage(UUID outboxId) {
        return new ClaimedOutboxMessage(
                outboxId,
                EVENT_TYPE,
                new byte[]{1, 2, 3},
                new byte[12],
                "local-v1",
                1
        );
    }
}