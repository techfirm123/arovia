package com.arovia.api.notifications.infrastructure;

import java.util.UUID;

import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.application.port.EmailOutboxPersistencePort;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

import org.springframework.stereotype.Component;

@Component
public class EmailOutboxPersistenceAdapter
        implements EmailOutboxPersistencePort {

    private final EmailOutboxStore outboxStore;

    public EmailOutboxPersistenceAdapter(EmailOutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    @Override
    public void enqueue(
            UUID outboxId,
            EmailNotificationType type,
            UUID aggregateId,
            EncryptedOutboxPayload encryptedPayload
    ) {
        outboxStore.enqueue(
                outboxId,
                type,
                aggregateId,
                encryptedPayload
        );
    }

    @Override
    public void supersedePending(
            EmailNotificationType type,
            UUID aggregateId
    ) {
        outboxStore.supersedePending(type, aggregateId);
    }
}