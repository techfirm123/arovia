
package com.arovia.api.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.identity.infrastructure.IdentityEmailVerificationResendStore;
import com.arovia.api.identity.infrastructure.VerificationTokenGenerator;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.infrastructure.OutboxPayloadCipher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdentityEmailVerificationResendService {

    private static final String VERIFICATION_EVENT_TYPE =
            "USER_EMAIL_VERIFICATION_REQUESTED";

    private final IdentityEmailVerificationResendStore resendStore;
    private final IdentityRequestRateLimiter requestRateLimiter;
    private final VerificationTokenGenerator tokenGenerator;
    private final OutboxPayloadCipher outboxPayloadCipher;
    private final Clock clock;
    private final Duration verificationTokenTtl;
    private final Duration resendCooldown;

    public IdentityEmailVerificationResendService(
            IdentityEmailVerificationResendStore resendStore,
            IdentityRequestRateLimiter requestRateLimiter,
            VerificationTokenGenerator tokenGenerator,
            OutboxPayloadCipher outboxPayloadCipher,
            Clock applicationClock,
            @Value("${arovia.identity.email-verification-ttl:PT24H}")
            Duration verificationTokenTtl,
            @Value("${arovia.identity.email-verification-resend-cooldown:PT1M}")
            Duration resendCooldown
    ) {
        this.resendStore = resendStore;
        this.requestRateLimiter = requestRateLimiter;
        this.tokenGenerator = tokenGenerator;
        this.outboxPayloadCipher = outboxPayloadCipher;
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
         * tokens or encrypting a new email payload.
         */
        if (!requestRateLimiter.allowResend(
                sourceIp,
                normalizedEmail
        )) {
            return;
        }

        UUID verificationTokenId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();

        String rawToken = tokenGenerator.generateToken();
        String tokenHash = tokenGenerator.hashToken(rawToken);

        /*
         * The recipient address is normalized using the same policy
         * as UserAccount registration. We avoid an additional account
         * lookup outside the transaction.
         */
        EmailVerificationEmailPayload emailPayload =
                new EmailVerificationEmailPayload(
                        normalizedEmail,
                        "there",
                        rawToken
                );

        EncryptedOutboxPayload encryptedPayload =
                outboxPayloadCipher.encrypt(
                        outboxId,
                        VERIFICATION_EVENT_TYPE,
                        emailPayload
                );

        EmailVerificationResendData resendData =
                new EmailVerificationResendData(
                        verificationTokenId,
                        tokenHash,
                        clock.instant().plus(verificationTokenTtl),
                        outboxId,
                        VERIFICATION_EVENT_TYPE,
                        encryptedPayload
                );

        /*
         * The store locks and revalidates the account.
         * If the account doesn't exist, is already verified,
         * or is still inside the cooldown, nothing is persisted.
         */
        resendStore.resendIfEligible(
                normalizedEmail,
                resendData,
                resendCooldown,
                sourceIp,
                userAgent
        );
    }
}
