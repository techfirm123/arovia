package com.arovia.api.notifications.application;

import java.util.Objects;
import java.util.UUID;

import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.api.NotificationOutbox;
import com.arovia.api.notifications.application.port.EmailOutboxPersistencePort;
import com.arovia.api.notifications.application.port.OutboxPayloadEncryptionPort;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxService implements NotificationOutbox {

    private final EmailOutboxPersistencePort outboxPersistence;
    private final OutboxPayloadEncryptionPort payloadEncryption;

    public NotificationOutboxService(
            EmailOutboxPersistencePort outboxPersistence,
            OutboxPayloadEncryptionPort payloadEncryption
    ) {
        this.outboxPersistence = outboxPersistence;
        this.payloadEncryption = payloadEncryption;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(
            UUID aggregateId,
            EmailNotificationRequest request
    ) {
        Objects.requireNonNull(
                aggregateId,
                "Notification aggregate ID is required."
        );

        Objects.requireNonNull(
                request,
                "Email notification request is required."
        );

        UUID outboxId = UUID.randomUUID();

        EncryptedOutboxPayload encryptedPayload = encryptPayload(
                outboxId,
                request
        );

        outboxPersistence.enqueue(
                outboxId,
                request.type(),
                aggregateId,
                encryptedPayload
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void supersedePending(
            EmailNotificationType type,
            UUID aggregateId
    ) {
        Objects.requireNonNull(
                type,
                "Notification type is required."
        );

        Objects.requireNonNull(
                aggregateId,
                "Notification aggregate ID is required."
        );

        outboxPersistence.supersedePending(type, aggregateId);
    }

    private EncryptedOutboxPayload encryptPayload(
            UUID outboxId,
            EmailNotificationRequest request
    ) {
        return switch (request.type()) {
            case EMAIL_VERIFICATION -> payloadEncryption.encrypt(
                    outboxId,
                    request.type().eventType(),
                    new EmailVerificationEmailPayload(
                            request.recipientEmail(),
                            request.recipientName(),
                            request.actionToken()
                    )
            );

            case PASSWORD_RESET -> payloadEncryption.encrypt(
                    outboxId,
                    request.type().eventType(),
                    new PasswordResetEmailPayload(
                            request.recipientEmail(),
                            request.recipientName(),
                            request.actionToken()
                    )
            );
        };
    }
}