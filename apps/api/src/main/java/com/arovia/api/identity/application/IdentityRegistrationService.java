
package com.arovia.api.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import com.arovia.api.identity.domain.RegistrationData;
import com.arovia.api.identity.infrastructure.IdentityRegistrationStore;
import com.arovia.api.identity.infrastructure.VerificationTokenGenerator;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.infrastructure.OutboxPayloadCipher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityRegistrationService {

    private static final String VERIFICATION_EVENT_TYPE =
            "USER_EMAIL_VERIFICATION_REQUESTED";

    private final IdentityRegistrationStore registrationStore;
    private final IdentityRequestRateLimiter requestRateLimiter;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenGenerator tokenGenerator;
    private final OutboxPayloadCipher outboxPayloadCipher;
    private final Clock clock;
    private final Duration verificationTokenTtl;

    public IdentityRegistrationService(
            IdentityRegistrationStore registrationStore,
            IdentityRequestRateLimiter requestRateLimiter,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            VerificationTokenGenerator tokenGenerator,
            OutboxPayloadCipher outboxPayloadCipher,
            Clock applicationClock,
            @Value("${arovia.identity.email-verification-ttl:PT24H}")
            Duration verificationTokenTtl
    ) {
        this.registrationStore = registrationStore;
        this.requestRateLimiter = requestRateLimiter;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.outboxPayloadCipher = outboxPayloadCipher;
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

        /*
         * Both IP and email limits are checked before Argon2 hashing.
         * A throttled request receives the same generic API response.
         */
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

        /*
         * Password hashing is intentionally performed before the
         * database transaction begins.
         */
        String passwordHash = passwordEncoder.encode(normalizedPassword);

        UUID userAccountId = UUID.randomUUID();
        UUID verificationTokenId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();

        String rawVerificationToken = tokenGenerator.generateToken();
        String verificationTokenHash = tokenGenerator.hashToken(
                rawVerificationToken
        );

        EmailVerificationEmailPayload emailPayload =
                new EmailVerificationEmailPayload(
                        email,
                        normalizedDisplayName,
                        rawVerificationToken
                );

        EncryptedOutboxPayload encryptedPayload = outboxPayloadCipher.encrypt(
                outboxId,
                VERIFICATION_EVENT_TYPE,
                emailPayload
        );

        RegistrationData registration = new RegistrationData(
                userAccountId,
                email,
                normalizedEmail,
                passwordHash,
                normalizedDisplayName,
                verificationTokenId,
                verificationTokenHash,
                clock.instant().plus(verificationTokenTtl),
                outboxId,
                VERIFICATION_EVENT_TYPE,
                encryptedPayload
        );

        registrationStore.createPendingAccount(
                registration,
                sourceIp,
                userAgent
        );
    }
}
