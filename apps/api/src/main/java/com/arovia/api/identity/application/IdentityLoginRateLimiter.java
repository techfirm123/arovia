package com.arovia.api.identity.application;

import java.time.Duration;

import com.arovia.api.identity.infrastructure.IdentityRateLimitStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdentityLoginRateLimiter {

    private static final String LOGIN_IP = "LOGIN_IP";
    private static final String LOGIN_EMAIL = "LOGIN_EMAIL";

    private final IdentityRateLimitStore rateLimitStore;

    private final Duration window;
    private final int ipLimit;
    private final int emailLimit;

    public IdentityLoginRateLimiter(
            IdentityRateLimitStore rateLimitStore,
            @Value("${arovia.rate-limit.login.window:PT15M}")
            Duration window,
            @Value("${arovia.rate-limit.login.ip-limit:60}")
            int ipLimit,
            @Value("${arovia.rate-limit.login.email-limit:10}")
            int emailLimit
    ) {
        this.rateLimitStore = rateLimitStore;
        this.window = window;
        this.ipLimit = ipLimit;
        this.emailLimit = emailLimit;
    }

    /*
     * The current policy counts login attempts, not only failed attempts.
     * Both dimensions are consumed even when one dimension is already
     * over its limit, keeping enforcement behavior predictable.
     */
    public boolean allowLogin(
            String sourceIp,
            String normalizedEmail
    ) {
        String safeIp = sourceIp == null || sourceIp.isBlank()
                ? "unknown"
                : sourceIp;

        boolean ipAllowed = rateLimitStore.consume(
                LOGIN_IP,
                safeIp,
                window,
                ipLimit
        );

        boolean emailAllowed = rateLimitStore.consume(
                LOGIN_EMAIL,
                normalizedEmail,
                window,
                emailLimit
        );

        return ipAllowed && emailAllowed;
    }
}