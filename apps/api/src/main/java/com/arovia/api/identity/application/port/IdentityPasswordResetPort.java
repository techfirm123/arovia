package com.arovia.api.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.notifications.api.EmailNotificationRequest;

public interface IdentityPasswordResetPort {

    Optional<AccountResetTarget> findEligibleAccount(
            String normalizedEmail
    );

    boolean createPasswordReset(
            UUID userAccountId,
            UUID resetTokenId,
            String tokenHash,
            Instant expiresAt,
            EmailNotificationRequest notificationRequest,
            String sourceIp,
            String userAgent
    );

    Optional<PasswordResetTarget> findValidResetToken(
            String tokenHash
    );

    boolean completePasswordReset(
            String tokenHash,
            String newPasswordHash,
            String sourceIp,
            String userAgent
    );

    record AccountResetTarget(
            UUID userAccountId,
            String email,
            String normalizedEmail,
            String displayName
    ) {
    }

    record PasswordResetTarget(
            UUID userAccountId,
            String normalizedEmail
    ) {
    }
}