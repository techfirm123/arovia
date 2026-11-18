package com.arovia.api.notifications.application.port;

import java.util.UUID;

import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

public interface EmailOutboxPersistencePort {

    void enqueue(
            UUID outboxId,
            EmailNotificationType type,
            UUID aggregateId,
            EncryptedOutboxPayload encryptedPayload
    );

    void supersedePending(
            EmailNotificationType type,
            UUID aggregateId
    );
}