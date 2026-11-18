package com.arovia.api.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.identity.application.port.IdentityPasswordResetPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort.AccountResetTarget;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort.PasswordResetTarget;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityPasswordResetService {

    private final IdentityPasswordResetPort resetPort;
    private final IdentityPasswordResetRateLimiter rateLimiter;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenProvider tokenProvider;
    private final Clock clock;
    private final Duration tokenTtl;

    public IdentityPasswordResetService(
            IdentityPasswordResetPort resetPort,
            IdentityPasswordResetRateLimiter rateLimiter,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            VerificationTokenProvider tokenProvider,
            Clock applicationClock,
            @Value("${arovia.identity.password-reset-ttl:PT30M}")
            Duration tokenTtl
    ) {
        this.resetPort = resetPort;
        this.rateLimiter = rateLimiter;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
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

        Optional<AccountResetTarget> account =
                resetPort.findEligibleAccount(normalizedEmail);

        /*
         * Do not disclose whether the email address belongs to
         * an active, verified account.
         */
        if (account.isEmpty()) {
            return;
        }

        AccountResetTarget target = account.get();

        UUID resetTokenId = UUID.randomUUID();

        String rawToken = tokenProvider.generateToken();
        String tokenHash = tokenProvider.hashToken(rawToken);

        EmailNotificationRequest notificationRequest =
                new EmailNotificationRequest(
                        EmailNotificationType.PASSWORD_RESET,
                        target.email(),
                        target.displayName(),
                        rawToken
                );

        resetPort.createPasswordReset(
                target.userAccountId(),
                resetTokenId,
                tokenHash,
                clock.instant().plus(tokenTtl),
                notificationRequest,
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

        String tokenHash = tokenProvider.hashToken(rawToken);

        PasswordResetTarget target =
                resetPort.findValidResetToken(tokenHash)
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

        boolean completed = resetPort.completePasswordReset(
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