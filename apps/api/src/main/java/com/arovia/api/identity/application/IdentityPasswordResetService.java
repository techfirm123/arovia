package com.arovia.api.identity.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.identity.infrastructure.IdentityPasswordResetStore;
import com.arovia.api.identity.infrastructure.VerificationTokenGenerator;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;
import com.arovia.api.notifications.infrastructure.OutboxPayloadCipher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityPasswordResetService {

    private static final String PASSWORD_RESET_EVENT_TYPE =
            "USER_PASSWORD_RESET_REQUESTED";

    private final IdentityPasswordResetStore resetStore;
    private final IdentityPasswordResetRateLimiter rateLimiter;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenGenerator tokenGenerator;
    private final OutboxPayloadCipher outboxPayloadCipher;
    private final Clock clock;
    private final Duration tokenTtl;

    public IdentityPasswordResetService(
            IdentityPasswordResetStore resetStore,
            IdentityPasswordResetRateLimiter rateLimiter,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            VerificationTokenGenerator tokenGenerator,
            OutboxPayloadCipher outboxPayloadCipher,
            Clock applicationClock,
            @Value("${arovia.identity.password-reset-ttl:PT30M}")
            Duration tokenTtl
    ) {
        this.resetStore = resetStore;
        this.rateLimiter = rateLimiter;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.outboxPayloadCipher = outboxPayloadCipher;
        this.clock = applicationClock;
        this.tokenTtl = tokenTtl;
    }

    public void requestReset(
            String suppliedEmail,
            String sourceIp,
            String userAgent
    ) {
        String normalizedEmail = suppliedEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        /*
         * Rate limiting is applied regardless of whether the
         * account exists.
         */
        if (!rateLimiter.allowRequest(
                sourceIp,
                normalizedEmail
        )) {
            return;
        }

        Optional<IdentityPasswordResetStore.AccountResetTarget> account =
                resetStore.findEligibleAccount(normalizedEmail);

        /*
         * Do not disclose whether the email address belongs to
         * an active, verified account.
         */
        if (account.isEmpty()) {
            return;
        }

        IdentityPasswordResetStore.AccountResetTarget target =
                account.get();

        UUID resetTokenId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();

        String rawToken = tokenGenerator.generateToken();

        String tokenHash = tokenGenerator.hashToken(rawToken);

        PasswordResetEmailPayload emailPayload =
                new PasswordResetEmailPayload(
                        target.email(),
                        target.displayName(),
                        rawToken
                );

        EncryptedOutboxPayload encryptedPayload =
                outboxPayloadCipher.encrypt(
                        outboxId,
                        PASSWORD_RESET_EVENT_TYPE,
                        emailPayload
                );

        resetStore.createPasswordReset(
                target.userAccountId(),
                resetTokenId,
                tokenHash,
                Timestamp.from(clock.instant().plus(tokenTtl)),
                outboxId,
                encryptedPayload,
                sourceIp,
                userAgent
        );
    }

    public void confirmReset(
            String rawToken,
            String suppliedNewPassword,
            String sourceIp,
            String userAgent
    ) {
        if (!rateLimiter.allowConfirmation(sourceIp)) {
            throw new PasswordResetRateLimitedException();
        }

        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }

        String tokenHash = tokenGenerator.hashToken(rawToken);

        IdentityPasswordResetStore.PasswordResetTarget target =
                resetStore.findValidResetToken(tokenHash)
                        .orElseThrow(
                                InvalidPasswordResetTokenException::new
                        );

        String normalizedPassword =
                passwordPolicy.validateAndNormalize(
                        suppliedNewPassword,
                        target.normalizedEmail()
                );

        String newPasswordHash =
                passwordEncoder.encode(normalizedPassword);

        boolean completed = resetStore.completePasswordReset(
                tokenHash,
                newPasswordHash,
                sourceIp,
                userAgent
        );

        if (!completed) {
            throw new InvalidPasswordResetTokenException();
        }
    }
}