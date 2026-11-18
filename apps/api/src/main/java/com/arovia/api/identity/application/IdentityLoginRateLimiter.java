package com.arovia.api.identity.application;

import com.arovia.api.identity.application.port.IdentityRateLimitPort;
import com.arovia.api.identity.configuration.IdentityRateLimitProperties;

import org.springframework.stereotype.Service;

@Service
public class IdentityLoginRateLimiter {

    private static final String LOGIN_IP = "LOGIN_IP";
    private static final String LOGIN_EMAIL = "LOGIN_EMAIL";

    private final IdentityRateLimitPort rateLimitPort;
    private final IdentityRateLimitProperties properties;

    public IdentityLoginRateLimiter(
            IdentityRateLimitPort rateLimitPort,
            IdentityRateLimitProperties properties
    ) {
        this.rateLimitPort = rateLimitPort;
        this.properties = properties;
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

        IdentityRateLimitProperties.RateLimitPolicy policy =
                properties.getLogin();

        boolean ipAllowed = rateLimitPort.consume(
                LOGIN_IP,
                safeIp,
                policy.getWindow(),
                policy.getIpLimit()
        );

        boolean emailAllowed = rateLimitPort.consume(
                LOGIN_EMAIL,
                normalizedEmail,
                policy.getWindow(),
                policy.getEmailLimit()
        );

        return ipAllowed && emailAllowed;
    }
}