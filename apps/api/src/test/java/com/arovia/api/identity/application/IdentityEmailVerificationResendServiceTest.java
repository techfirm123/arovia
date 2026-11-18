package com.arovia.api.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.arovia.api.identity.application.port.IdentityEmailVerificationResendPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;
import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityEmailVerificationResendServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-13T12:00:00Z");

    @Mock
    private IdentityEmailVerificationResendPort resendPort;

    @Mock
    private IdentityRequestRateLimiter requestRateLimiter;

    @Mock
    private VerificationTokenProvider tokenProvider;

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
                tokenProvider,
                resendPort
        );
    }

    @Test
    void normalizesEmailAndCreatesReplacementNotificationRequest() {
        when(requestRateLimiter.allowResend(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(true);

        when(tokenProvider.generateToken())
                .thenReturn("new-verification-token");

        when(tokenProvider.hashToken("new-verification-token"))
                .thenReturn("new-token-hash");

        IdentityEmailVerificationResendService service = createService();

        service.resend(
                " Person@Example.com ",
                "127.0.0.1",
                "Arovia test client"
        );

        ArgumentCaptor<EmailVerificationResendData> dataCaptor =
                ArgumentCaptor.forClass(
                        EmailVerificationResendData.class
                );

        ArgumentCaptor<EmailNotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(
                        EmailNotificationRequest.class
                );

        verify(resendPort).resendIfEligible(
                eq("person@example.com"),
                dataCaptor.capture(),
                notificationCaptor.capture(),
                eq(Duration.ofMinutes(1)),
                eq("127.0.0.1"),
                eq("Arovia test client")
        );

        EmailVerificationResendData resendData = dataCaptor.getValue();

        assertEquals(
                "new-token-hash",
                resendData.verificationTokenHash()
        );

        assertEquals(
                NOW.plus(Duration.ofHours(24)),
                resendData.verificationTokenExpiresAt()
        );

        EmailNotificationRequest notification =
                notificationCaptor.getValue();

        assertEquals(
                EmailNotificationType.EMAIL_VERIFICATION,
                notification.type()
        );

        assertEquals(
                "person@example.com",
                notification.recipientEmail()
        );

        assertEquals("there", notification.recipientName());

        assertEquals(
                "new-verification-token",
                notification.actionToken()
        );

        verify(tokenProvider).generateToken();
        verify(tokenProvider).hashToken("new-verification-token");
    }

    private IdentityEmailVerificationResendService createService() {
        return new IdentityEmailVerificationResendService(
                resendPort,
                requestRateLimiter,
                tokenProvider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofHours(24),
                Duration.ofMinutes(1)
        );
    }
}