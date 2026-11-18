package com.arovia.api.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import com.arovia.api.identity.application.port.IdentityEmailVerificationResendPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;
import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdentityEmailVerificationResendService {

    private final IdentityEmailVerificationResendPort resendPort;
    private final IdentityRequestRateLimiter requestRateLimiter;
    private final VerificationTokenProvider tokenProvider;
    private final Clock clock;
    private final Duration verificationTokenTtl;
    private final Duration resendCooldown;

    public IdentityEmailVerificationResendService(
            IdentityEmailVerificationResendPort resendPort,
            IdentityRequestRateLimiter requestRateLimiter,
            VerificationTokenProvider tokenProvider,
            Clock applicationClock,
            @Value("${arovia.identity.email-verification-ttl:PT24H}")
            Duration verificationTokenTtl,
            @Value("${arovia.identity.email-verification-resend-cooldown:PT1M}")
            Duration resendCooldown
    ) {
        this.resendPort = resendPort;
        this.requestRateLimiter = requestRateLimiter;
        this.tokenProvider = tokenProvider;
        this.clock = applicationClock;
        this.verificationTokenTtl = verificationTokenTtl;
        this.resendCooldown = resendCooldown;
    }

    public void resend(
            String suppliedEmail,
            String sourceIp,
            String userAgent
    ) {
        String normalizedEmail = suppliedEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        /*
         * Apply distributed IP and email limits before generating
         * a replacement token.
         */
        if (!requestRateLimiter.allowResend(
                sourceIp,
                normalizedEmail
        )) {
            return;
        }

        String rawToken = tokenProvider.generateToken();
        String tokenHash = tokenProvider.hashToken(rawToken);

        EmailVerificationResendData resendData =
                new EmailVerificationResendData(
                        UUID.randomUUID(),
                        tokenHash,
                        clock.instant().plus(verificationTokenTtl)
                );

        EmailNotificationRequest notificationRequest =
                new EmailNotificationRequest(
                        EmailNotificationType.EMAIL_VERIFICATION,
                        normalizedEmail,
                        "there",
                        rawToken
                );

        /*
         * The persistence adapter locks and revalidates the account.
         * It then supersedes older queued notifications, rotates the
         * token, updates the cooldown timestamp and enqueues the new
         * notification in one transaction.
         */
        resendPort.resendIfEligible(
                normalizedEmail,
                resendData,
                notificationRequest,
                resendCooldown,
                sourceIp,
                userAgent
        );
    }
}