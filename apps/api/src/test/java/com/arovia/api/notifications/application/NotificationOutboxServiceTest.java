package com.arovia.api.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.application.port.EmailOutboxPersistencePort;
import com.arovia.api.notifications.application.port.OutboxPayloadEncryptionPort;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock
    private EmailOutboxPersistencePort outboxPersistence;

    @Mock
    private OutboxPayloadEncryptionPort payloadEncryption;

    private NotificationOutboxService notificationOutbox;

    @BeforeEach
    void setUp() {
        notificationOutbox = new NotificationOutboxService(
                outboxPersistence,
                payloadEncryption
        );
    }

    @Test
    void shouldEncryptAndEnqueueEmailVerificationNotification() {
        UUID aggregateId = UUID.randomUUID();

        EmailNotificationRequest request =
                new EmailNotificationRequest(
                        EmailNotificationType.EMAIL_VERIFICATION,
                        "person@example.com",
                        "Arovia User",
                        "verification-token"
                );

        EncryptedOutboxPayload encryptedPayload =
                new EncryptedOutboxPayload(
                        new byte[]{1, 2, 3},
                        new byte[12],
                        "test-key-v1"
                );

        when(payloadEncryption.encrypt(
                any(UUID.class),
                eq(EmailNotificationType.EMAIL_VERIFICATION.eventType()),
                any(EmailVerificationEmailPayload.class)
        )).thenReturn(encryptedPayload);

        notificationOutbox.enqueue(aggregateId, request);

        ArgumentCaptor<EmailVerificationEmailPayload> payloadCaptor =
                ArgumentCaptor.forClass(
                        EmailVerificationEmailPayload.class
                );

        ArgumentCaptor<UUID> outboxIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        verify(payloadEncryption).encrypt(
                outboxIdCaptor.capture(),
                eq(EmailNotificationType.EMAIL_VERIFICATION.eventType()),
                payloadCaptor.capture()
        );

        EmailVerificationEmailPayload payload = payloadCaptor.getValue();

        assertNotNull(outboxIdCaptor.getValue());
        assertEquals("person@example.com", payload.recipientEmail());
        assertEquals("Arovia User", payload.displayName());
        assertEquals("verification-token", payload.verificationToken());

        verify(outboxPersistence).enqueue(
                outboxIdCaptor.getValue(),
                EmailNotificationType.EMAIL_VERIFICATION,
                aggregateId,
                encryptedPayload
        );
    }

    @Test
    void shouldEncryptAndEnqueuePasswordResetNotification() {
        UUID aggregateId = UUID.randomUUID();

        EmailNotificationRequest request =
                new EmailNotificationRequest(
                        EmailNotificationType.PASSWORD_RESET,
                        "person@example.com",
                        "Arovia User",
                        "password-reset-token"
                );

        EncryptedOutboxPayload encryptedPayload =
                new EncryptedOutboxPayload(
                        new byte[]{4, 5, 6},
                        new byte[12],
                        "test-key-v1"
                );

        when(payloadEncryption.encrypt(
                any(UUID.class),
                eq(EmailNotificationType.PASSWORD_RESET.eventType()),
                any(PasswordResetEmailPayload.class)
        )).thenReturn(encryptedPayload);

        notificationOutbox.enqueue(aggregateId, request);

        ArgumentCaptor<PasswordResetEmailPayload> payloadCaptor =
                ArgumentCaptor.forClass(
                        PasswordResetEmailPayload.class
                );

        ArgumentCaptor<UUID> outboxIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        verify(payloadEncryption).encrypt(
                outboxIdCaptor.capture(),
                eq(EmailNotificationType.PASSWORD_RESET.eventType()),
                payloadCaptor.capture()
        );

        PasswordResetEmailPayload payload = payloadCaptor.getValue();

        assertNotNull(outboxIdCaptor.getValue());
        assertEquals("person@example.com", payload.recipientEmail());
        assertEquals("Arovia User", payload.displayName());
        assertEquals("password-reset-token", payload.resetToken());

        verify(outboxPersistence).enqueue(
                outboxIdCaptor.getValue(),
                EmailNotificationType.PASSWORD_RESET,
                aggregateId,
                encryptedPayload
        );
    }

    @Test
    void shouldRejectNullAggregateId() {
        EmailNotificationRequest request =
                new EmailNotificationRequest(
                        EmailNotificationType.EMAIL_VERIFICATION,
                        "person@example.com",
                        "Arovia User",
                        "verification-token"
                );

        assertThrows(
                NullPointerException.class,
                () -> notificationOutbox.enqueue(null, request)
        );
    }

    @Test
    void shouldRejectNullNotificationRequest() {
        assertThrows(
                NullPointerException.class,
                () -> notificationOutbox.enqueue(UUID.randomUUID(), null)
        );
    }

    @Test
    void shouldDelegateSupersedingPendingNotifications() {
        UUID aggregateId = UUID.randomUUID();

        notificationOutbox.supersedePending(
                EmailNotificationType.EMAIL_VERIFICATION,
                aggregateId
        );

        verify(outboxPersistence).supersedePending(
                EmailNotificationType.EMAIL_VERIFICATION,
                aggregateId
        );
    }

    @Test
    void shouldRequireAnExistingTransactionForEnqueue() throws Exception {
        Transactional transactional =
                NotificationOutboxService.class
                        .getMethod(
                                "enqueue",
                                UUID.class,
                                EmailNotificationRequest.class
                        )
                        .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(
                Propagation.MANDATORY,
                transactional.propagation()
        );
    }

    @Test
    void shouldRequireAnExistingTransactionForSuperseding()
            throws Exception {
        Transactional transactional =
                NotificationOutboxService.class
                        .getMethod(
                                "supersedePending",
                                EmailNotificationType.class,
                                UUID.class
                        )
                        .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(
                Propagation.MANDATORY,
                transactional.propagation()
        );
    }
}