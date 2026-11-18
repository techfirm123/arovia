package com.arovia.api.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationResendData(
        UUID verificationTokenId,
        String verificationTokenHash,
        Instant verificationTokenExpiresAt
) {
}