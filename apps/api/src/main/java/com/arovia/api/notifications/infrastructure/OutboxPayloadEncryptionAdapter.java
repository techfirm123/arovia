package com.arovia.api.notifications.infrastructure;

import java.util.UUID;

import com.arovia.api.notifications.application.port.OutboxPayloadEncryptionPort;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

import org.springframework.stereotype.Component;

@Component
public class OutboxPayloadEncryptionAdapter
        implements OutboxPayloadEncryptionPort {

    private final OutboxPayloadCipher payloadCipher;

    public OutboxPayloadEncryptionAdapter(
            OutboxPayloadCipher payloadCipher
    ) {
        this.payloadCipher = payloadCipher;
    }

    @Override
    public EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            EmailVerificationEmailPayload payload
    ) {
        return payloadCipher.encrypt(outboxId, eventType, payload);
    }

    @Override
    public EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            PasswordResetEmailPayload payload
    ) {
        return payloadCipher.encrypt(outboxId, eventType, payload);
    }
}