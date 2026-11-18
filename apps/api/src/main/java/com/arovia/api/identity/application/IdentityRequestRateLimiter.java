package com.arovia.api.identity.application;

import com.arovia.api.identity.application.port.IdentityRateLimitPort;
import com.arovia.api.identity.configuration.IdentityRateLimitProperties;

import org.springframework.stereotype.Service;

@Service
public class IdentityRequestRateLimiter {

    private static final String REGISTRATION_IP = "REGISTRATION_IP";
    private static final String REGISTRATION_EMAIL = "REGISTRATION_EMAIL";

    private static final String RESEND_IP = "VERIFICATION_RESEND_IP";
    private static final String RESEND_EMAIL = "VERIFICATION_RESEND_EMAIL";

    private final IdentityRateLimitPort rateLimitPort;
    private final IdentityRateLimitProperties properties;

    public IdentityRequestRateLimiter(
            IdentityRateLimitPort rateLimitPort,
            IdentityRateLimitProperties properties
    ) {
        this.rateLimitPort = rateLimitPort;
        this.properties = properties;
    }

    public boolean allowRegistration(
            String sourceIp,
            String normalizedEmail
    ) {
        IdentityRateLimitProperties.RateLimitPolicy policy =
                properties.getRegistration();

        boolean ipAllowed = rateLimitPort.consume(
                REGISTRATION_IP,
                sourceIp,
                policy.getWindow(),
                policy.getIpLimit()
        );

        boolean emailAllowed = rateLimitPort.consume(
                REGISTRATION_EMAIL,
                normalizedEmail,
                policy.getWindow(),
                policy.getEmailLimit()
        );

        return ipAllowed && emailAllowed;
    }

    public boolean allowResend(
            String sourceIp,
            String normalizedEmail
    ) {
        IdentityRateLimitProperties.RateLimitPolicy policy =
                properties.getResend();

        boolean ipAllowed = rateLimitPort.consume(
                RESEND_IP,
                sourceIp,
                policy.getWindow(),
                policy.getIpLimit()
        );

        boolean emailAllowed = rateLimitPort.consume(
                RESEND_EMAIL,
                normalizedEmail,
                policy.getWindow(),
                policy.getEmailLimit()
        );

        return ipAllowed && emailAllowed;
    }
}