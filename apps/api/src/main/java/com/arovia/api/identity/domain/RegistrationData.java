
package com.arovia.api.identity.domain;

import java.time.Instant;
import java.util.UUID;

import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

public record RegistrationData(
        UUID userAccountId,
        String email,
        String normalizedEmail,
        String passwordHash,
        String displayName,
        UUID verificationTokenId,
        String verificationTokenHash,
        Instant verificationTokenExpiresAt,
        UUID outboxId,
        String outboxEventType,
        EncryptedOutboxPayload encryptedOutboxPayload
) {
}
