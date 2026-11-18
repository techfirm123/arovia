package com.arovia.api.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.identity.application.port.IdentityPasswordResetPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort.AccountResetTarget;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort.PasswordResetTarget;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class IdentityPasswordResetServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-18T08:00:00Z");

    @Mock
    private IdentityPasswordResetPort resetPort;

    @Mock
    private IdentityPasswordResetRateLimiter rateLimiter;

    @Mock
    private PasswordPolicy passwordPolicy;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenProvider tokenProvider;

    @Test
    void requestResetDoesNothingWhenRateLimitIsExceeded() {
        when(rateLimiter.allowRequest(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(false);

        IdentityPasswordResetService service = createService();

        service.requestReset(
                " Person@Example.com ",
                "127.0.0.1",
                "Arovia test client"
        );

        verify(resetPort, never()).findEligibleAccount(any());
        verify(tokenProvider, never()).generateToken();
    }

    @Test
    void requestResetCreatesTokenAndNotificationForEligibleAccount() {
        UUID userAccountId = UUID.randomUUID();

        when(rateLimiter.allowRequest(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(true);

        when(resetPort.findEligibleAccount("person@example.com"))
                .thenReturn(Optional.of(
                        new AccountResetTarget(
                                userAccountId,
                                "Person@example.com",
                                "person@example.com",
                                "Person"
                        )
                ));

        when(tokenProvider.generateToken())
                .thenReturn("password-reset-token");

        when(tokenProvider.hashToken("password-reset-token"))
                .thenReturn("password-reset-token-hash");

        IdentityPasswordResetService service = createService();

        service.requestReset(
                " Person@Example.com ",
                "127.0.0.1",
                "Arovia test client"
        );

        ArgumentCaptor<UUID> tokenIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        ArgumentCaptor<Instant> expiryCaptor =
                ArgumentCaptor.forClass(Instant.class);

        ArgumentCaptor<EmailNotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);

        verify(resetPort).createPasswordReset(
                eq(userAccountId),
                tokenIdCaptor.capture(),
                eq("password-reset-token-hash"),
                expiryCaptor.capture(),
                notificationCaptor.capture(),
                eq("127.0.0.1"),
                eq("Arovia test client")
        );

        assertEquals(
                NOW.plus(Duration.ofMinutes(30)),
                expiryCaptor.getValue()
        );

        EmailNotificationRequest notification =
                notificationCaptor.getValue();

        assertEquals(
                EmailNotificationType.PASSWORD_RESET,
                notification.type()
        );

        assertEquals(
                "Person@example.com",
                notification.recipientEmail()
        );

        assertEquals("Person", notification.recipientName());

        assertEquals(
                "password-reset-token",
                notification.actionToken()
        );
    }

    @Test
    void confirmResetHashesTokenAndCompletesPasswordReset() {
        when(rateLimiter.allowConfirmation("127.0.0.1"))
                .thenReturn(true);

        when(tokenProvider.hashToken("submitted-reset-token"))
                .thenReturn("submitted-token-hash");

        when(resetPort.findValidResetToken("submitted-token-hash"))
                .thenReturn(Optional.of(
                        new PasswordResetTarget(
                                UUID.randomUUID(),
                                "person@example.com"
                        )
                ));

        when(passwordPolicy.validateAndNormalize(
                "a-valid-new-password",
                "person@example.com"
        )).thenReturn("normalized-new-password");

        when(passwordEncoder.encode("normalized-new-password"))
                .thenReturn("encoded-new-password");

        when(resetPort.completePasswordReset(
                "submitted-token-hash",
                "encoded-new-password",
                "127.0.0.1",
                "Arovia test client"
        )).thenReturn(true);

        IdentityPasswordResetService service = createService();

        service.confirmReset(
                "submitted-reset-token",
                "a-valid-new-password",
                "127.0.0.1",
                "Arovia test client"
        );

        verify(resetPort).completePasswordReset(
                "submitted-token-hash",
                "encoded-new-password",
                "127.0.0.1",
                "Arovia test client"
        );
    }

    private IdentityPasswordResetService createService() {
        return new IdentityPasswordResetService(
                resetPort,
                rateLimiter,
                passwordPolicy,
                passwordEncoder,
                tokenProvider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(30)
        );
    }
}