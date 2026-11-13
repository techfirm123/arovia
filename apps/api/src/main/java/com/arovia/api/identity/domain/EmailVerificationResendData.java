
package com.arovia.api.identity.domain;

import java.time.Instant;
import java.util.UUID;

import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

public record EmailVerificationResendData(
        UUID verificationTokenId,
        String verificationTokenHash,
        Instant verificationTokenExpiresAt,
        UUID outboxId,
        String outboxEventType,
        EncryptedOutboxPayload encryptedOutboxPayload
) {
}
