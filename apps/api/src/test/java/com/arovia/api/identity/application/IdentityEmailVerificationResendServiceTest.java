
package com.arovia.api.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.identity.infrastructure.IdentityEmailVerificationResendStore;
import com.arovia.api.identity.infrastructure.VerificationTokenGenerator;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.infrastructure.OutboxPayloadCipher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityEmailVerificationResendServiceTest {

    private static final String EVENT_TYPE =
            "USER_EMAIL_VERIFICATION_REQUESTED";

    private static final Instant NOW =
            Instant.parse("2026-11-13T12:00:00Z");

    @Mock
    private IdentityEmailVerificationResendStore resendStore;

    @Mock
    private IdentityRequestRateLimiter requestRateLimiter;

    @Mock
    private VerificationTokenGenerator tokenGenerator;

    @Mock
    private OutboxPayloadCipher outboxPayloadCipher;

    @Test
    void doesNotGenerateTokenWhenRateLimitIsExceeded() {
        when(requestRateLimiter.allowResend(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(false);

        IdentityEmailVerificationResendService service = createService();

        service.resend(
                " Person@Example.com ",
                "127.0.0.1",
                "Arovia test client"
        );

        verifyNoInteractions(
                tokenGenerator,
                outboxPayloadCipher,
                resendStore
        );
    }

    @Test
    void normalizesEmailAndCreatesEncryptedReplacementRequest() {
        when(requestRateLimiter.allowResend(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(true);

        when(tokenGenerator.generateToken())
                .thenReturn("new-verification-token");

        when(tokenGenerator.hashToken("new-verification-token"))
                .thenReturn("new-token-hash");

        EncryptedOutboxPayload encryptedPayload =
                new EncryptedOutboxPayload(
                        new byte[]{1, 2, 3},
                        new byte[12],
                        "test-v1"
                );

        when(outboxPayloadCipher.encrypt(
                any(UUID.class),
                eq(EVENT_TYPE),
                any(EmailVerificationEmailPayload.class)
        )).thenReturn(encryptedPayload);

        when(resendStore.resendIfEligible(
                eq("person@example.com"),
                any(EmailVerificationResendData.class),
                eq(Duration.ofMinutes(1)),
                eq("127.0.0.1"),
                eq("Arovia test client")
        )).thenReturn(true);

        IdentityEmailVerificationResendService service = createService();

        service.resend(
                " Person@Example.com ",
                "127.0.0.1",
                "Arovia test client"
        );

        ArgumentCaptor<EmailVerificationEmailPayload> payloadCaptor =
                ArgumentCaptor.forClass(
                        EmailVerificationEmailPayload.class
                );

        verify(outboxPayloadCipher).encrypt(
                any(UUID.class),
                eq(EVENT_TYPE),
                payloadCaptor.capture()
        );

        EmailVerificationEmailPayload emailPayload =
                payloadCaptor.getValue();

        assertEquals(
                "person@example.com",
                emailPayload.recipientEmail()
        );

        assertEquals("there", emailPayload.displayName());

        assertEquals(
                "new-verification-token",
                emailPayload.verificationToken()
        );

        ArgumentCaptor<EmailVerificationResendData> dataCaptor =
                ArgumentCaptor.forClass(
                        EmailVerificationResendData.class
                );

        verify(resendStore).resendIfEligible(
                eq("person@example.com"),
                dataCaptor.capture(),
                eq(Duration.ofMinutes(1)),
                eq("127.0.0.1"),
                eq("Arovia test client")
        );

        EmailVerificationResendData resendData = dataCaptor.getValue();

        assertEquals("new-token-hash", resendData.verificationTokenHash());
        assertEquals(NOW.plus(Duration.ofHours(24)),
                resendData.verificationTokenExpiresAt());

        verify(tokenGenerator).generateToken();
        verify(tokenGenerator).hashToken("new-verification-token");
    }

    private IdentityEmailVerificationResendService createService() {
        return new IdentityEmailVerificationResendService(
                resendStore,
                requestRateLimiter,
                tokenGenerator,
                outboxPayloadCipher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofHours(24),
                Duration.ofMinutes(1)
        );
    }
}
