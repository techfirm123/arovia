package com.arovia.api.identity.application;

import java.time.Duration;

import com.arovia.api.identity.infrastructure.IdentityRateLimitStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IdentityPasswordResetRateLimiter {

    private final IdentityRateLimitStore rateLimitStore;

    private final Duration window;
    private final int ipLimit;
    private final int emailLimit;
    private final int confirmationLimit;

    public IdentityPasswordResetRateLimiter(
            IdentityRateLimitStore rateLimitStore,
            @Value("${arovia.rate-limit.password-reset.window:PT1H}")
            Duration window,
            @Value("${arovia.rate-limit.password-reset.ip-limit:20}")
            int ipLimit,
            @Value("${arovia.rate-limit.password-reset.email-limit:5}")
            int emailLimit,
            @Value("${arovia.rate-limit.password-reset.confirmation-limit:10}")
            int confirmationLimit
    ) {
        this.rateLimitStore = rateLimitStore;
        this.window = window;
        this.ipLimit = ipLimit;
        this.emailLimit = emailLimit;
        this.confirmationLimit = confirmationLimit;
    }

    public boolean allowRequest(
            String sourceIp,
            String normalizedEmail
    ) {
        boolean ipAllowed = rateLimitStore.consume(
                "PASSWORD_RESET_IP",
                sourceIp,
                window,
                ipLimit
        );

        boolean emailAllowed = rateLimitStore.consume(
                "PASSWORD_RESET_EMAIL",
                normalizedEmail,
                window,
                emailLimit
        );

        return ipAllowed && emailAllowed;
    }

    public boolean allowConfirmation(String sourceIp) {
        return rateLimitStore.consume(
                "PASSWORD_RESET_CONFIRM_IP",
                sourceIp,
                window,
                confirmationLimit
        );
    }
}