package com.arovia.api.notifications.application.port;

import java.util.UUID;

import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

public interface OutboxPayloadEncryptionPort {

    EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            EmailVerificationEmailPayload payload
    );

    EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            PasswordResetEmailPayload payload
    );
}