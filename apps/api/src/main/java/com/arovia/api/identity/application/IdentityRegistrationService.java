package com.arovia.api.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import com.arovia.api.identity.application.port.IdentityRegistrationPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;
import com.arovia.api.identity.domain.RegistrationData;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityRegistrationService {

    private final IdentityRegistrationPort registrationPort;
    private final IdentityRequestRateLimiter requestRateLimiter;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenProvider tokenProvider;
    private final Clock clock;
    private final Duration verificationTokenTtl;

    public IdentityRegistrationService(
            IdentityRegistrationPort registrationPort,
            IdentityRequestRateLimiter requestRateLimiter,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            VerificationTokenProvider tokenProvider,
            Clock applicationClock,
            @Value("${arovia.identity.email-verification-ttl:PT24H}")
            Duration verificationTokenTtl
    ) {
        this.registrationPort = registrationPort;
        this.requestRateLimiter = requestRateLimiter;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.clock = applicationClock;
        this.verificationTokenTtl = verificationTokenTtl;
    }

    public void register(
            String displayName,
            String suppliedEmail,
            String suppliedPassword,
            String sourceIp,
            String userAgent
    ) {
        String email = suppliedEmail.trim();
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        String normalizedDisplayName = displayName.strip();

        if (!requestRateLimiter.allowRegistration(
                sourceIp,
                normalizedEmail
        )) {
            return;
        }

        String normalizedPassword = passwordPolicy.validateAndNormalize(
                suppliedPassword,
                normalizedEmail
        );

        String passwordHash = passwordEncoder.encode(normalizedPassword);

        UUID userAccountId = UUID.randomUUID();
        UUID verificationTokenId = UUID.randomUUID();

        String rawVerificationToken = tokenProvider.generateToken();
        String verificationTokenHash = tokenProvider.hashToken(
                rawVerificationToken
        );

        RegistrationData registration = new RegistrationData(
                userAccountId,
                email,
                normalizedEmail,
                passwordHash,
                normalizedDisplayName,
                verificationTokenId,
                verificationTokenHash,
                clock.instant().plus(verificationTokenTtl)
        );

        EmailNotificationRequest notificationRequest =
                new EmailNotificationRequest(
                        EmailNotificationType.EMAIL_VERIFICATION,
                        email,
                        normalizedDisplayName,
                        rawVerificationToken
                );

        registrationPort.createPendingAccount(
                registration,
                notificationRequest,
                sourceIp,
                userAgent
        );
    }
}