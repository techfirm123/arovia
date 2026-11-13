
package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.notifications.domain.ClaimedOutboxMessage;
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
            "USER_EMAIL_VERIFICATION_REQUESTED";

    @Mock
    private EmailOutboxStore outboxStore;

    @Mock
    private OutboxPayloadCipher payloadCipher;

    @Mock
    private JavaMailSender mailSender;

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
                org.mockito.ArgumentMatchers.eq(outboxId),
                any(),
                org.mockito.ArgumentMatchers.eq("SMTP_DELIVERY_FAILED")
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
                "UNSUPPORTED_EVENT_TYPE"
        );
    }

    private EmailOutboxDispatcher createDispatcher() {
        return new EmailOutboxDispatcher(
                outboxStore,
                payloadCipher,
                mailSender,
                Clock.systemUTC(),
                "http://localhost:3000",
                "no-reply@arovia.local",
                Duration.ofHours(24),
                1
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
