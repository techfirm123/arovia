package com.arovia.api.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RegistrationData(
        UUID userAccountId,
        String email,
        String normalizedEmail,
        String passwordHash,
        String displayName,
        UUID verificationTokenId,
        String verificationTokenHash,
        Instant verificationTokenExpiresAt
) {
}