
package com.arovia.api.identity.application;

import java.time.Duration;

import com.arovia.api.identity.infrastructure.IdentityRateLimitStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdentityRequestRateLimiter {

    private static final String REGISTRATION_IP = "REGISTRATION_IP";
    private static final String REGISTRATION_EMAIL = "REGISTRATION_EMAIL";

    private static final String RESEND_IP = "VERIFICATION_RESEND_IP";
    private static final String RESEND_EMAIL = "VERIFICATION_RESEND_EMAIL";

    private final IdentityRateLimitStore rateLimitStore;

    private final Duration registrationWindow;
    private final int registrationIpLimit;
    private final int registrationEmailLimit;

    private final Duration resendWindow;
    private final int resendIpLimit;
    private final int resendEmailLimit;

    public IdentityRequestRateLimiter(
            IdentityRateLimitStore rateLimitStore,
            @Value("${arovia.rate-limit.registration.window:PT1H}")
            Duration registrationWindow,
            @Value("${arovia.rate-limit.registration.ip-limit:10}")
            int registrationIpLimit,
            @Value("${arovia.rate-limit.registration.email-limit:3}")
            int registrationEmailLimit,
            @Value("${arovia.rate-limit.resend.window:PT1H}")
            Duration resendWindow,
            @Value("${arovia.rate-limit.resend.ip-limit:20}")
            int resendIpLimit,
            @Value("${arovia.rate-limit.resend.email-limit:5}")
            int resendEmailLimit
    ) {
        this.rateLimitStore = rateLimitStore;
        this.registrationWindow = registrationWindow;
        this.registrationIpLimit = registrationIpLimit;
        this.registrationEmailLimit = registrationEmailLimit;
        this.resendWindow = resendWindow;
        this.resendIpLimit = resendIpLimit;
        this.resendEmailLimit = resendEmailLimit;
    }

    public boolean allowRegistration(
            String sourceIp,
            String normalizedEmail
    ) {
        boolean ipAllowed = rateLimitStore.consume(
                REGISTRATION_IP,
                sourceIp,
                registrationWindow,
                registrationIpLimit
        );

        boolean emailAllowed = rateLimitStore.consume(
                REGISTRATION_EMAIL,
                normalizedEmail,
                registrationWindow,
                registrationEmailLimit
        );

        return ipAllowed && emailAllowed;
    }

    public boolean allowResend(
            String sourceIp,
            String normalizedEmail
    ) {
        boolean ipAllowed = rateLimitStore.consume(
                RESEND_IP,
                sourceIp,
                resendWindow,
                resendIpLimit
        );

        boolean emailAllowed = rateLimitStore.consume(
                RESEND_EMAIL,
                normalizedEmail,
                resendWindow,
                resendEmailLimit
        );

        return ipAllowed && emailAllowed;
    }
}
