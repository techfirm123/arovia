package com.arovia.api.identity.application;

import com.arovia.api.identity.application.port.IdentityRateLimitPort;
import com.arovia.api.identity.configuration.IdentityRateLimitProperties;

import org.springframework.stereotype.Component;

@Component
public class IdentityPasswordResetRateLimiter {

    private static final String PASSWORD_RESET_IP =
            "PASSWORD_RESET_IP";

    private static final String PASSWORD_RESET_EMAIL =
            "PASSWORD_RESET_EMAIL";

    private static final String PASSWORD_RESET_CONFIRM_IP =
            "PASSWORD_RESET_CONFIRM_IP";

    private final IdentityRateLimitPort rateLimitPort;
    private final IdentityRateLimitProperties properties;

    public IdentityPasswordResetRateLimiter(
            IdentityRateLimitPort rateLimitPort,
            IdentityRateLimitProperties properties
    ) {
        this.rateLimitPort = rateLimitPort;
        this.properties = properties;
    }

    public boolean allowRequest(
            String sourceIp,
            String normalizedEmail
    ) {
        IdentityRateLimitProperties.PasswordResetRateLimitPolicy policy =
                properties.getPasswordReset();

        boolean ipAllowed = rateLimitPort.consume(
                PASSWORD_RESET_IP,
                sourceIp,
                policy.getWindow(),
                policy.getIpLimit()
        );

        boolean emailAllowed = rateLimitPort.consume(
                PASSWORD_RESET_EMAIL,
                normalizedEmail,
                policy.getWindow(),
                policy.getEmailLimit()
        );

        return ipAllowed && emailAllowed;
    }

    public boolean allowConfirmation(String sourceIp) {
        IdentityRateLimitProperties.PasswordResetRateLimitPolicy policy =
                properties.getPasswordReset();

        return rateLimitPort.consume(
                PASSWORD_RESET_CONFIRM_IP,
                sourceIp,
                policy.getWindow(),
                policy.getConfirmationLimit()
        );
    }
}