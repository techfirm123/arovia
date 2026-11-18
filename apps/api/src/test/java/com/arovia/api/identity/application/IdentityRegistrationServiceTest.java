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

import com.arovia.api.identity.application.port.IdentityRegistrationPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;
import com.arovia.api.identity.domain.RegistrationData;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class IdentityRegistrationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-18T00:00:00Z");

    @Mock
    private IdentityRegistrationPort registrationPort;

    @Mock
    private IdentityRequestRateLimiter requestRateLimiter;

    @Mock
    private PasswordPolicy passwordPolicy;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenProvider tokenProvider;

    @Test
    void doesNotHashPasswordOrPersistWhenRegistrationIsRateLimited() {
        when(requestRateLimiter.allowRegistration(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(false);

        IdentityRegistrationService service = createService();

        service.register(
                "Person",
                " Person@Example.com ",
                "StrongPassword!123",
                "127.0.0.1",
                "Arovia test client"
        );

        verifyNoInteractions(
                passwordPolicy,
                passwordEncoder,
                tokenProvider,
                registrationPort
        );
    }

    @Test
    void normalizesInputAndDelegatesRegistrationToOutboundPort() {
        when(requestRateLimiter.allowRegistration(
                "127.0.0.1",
                "person@example.com"
        )).thenReturn(true);

        when(passwordPolicy.validateAndNormalize(
                "StrongPassword!123",
                "person@example.com"
        )).thenReturn("StrongPassword!123");

        when(passwordEncoder.encode("StrongPassword!123"))
                .thenReturn("encoded-password");

        when(tokenProvider.generateToken())
                .thenReturn("verification-token");

        when(tokenProvider.hashToken("verification-token"))
                .thenReturn("verification-token-hash");

        IdentityRegistrationService service = createService();

        service.register(
                "  Person Example  ",
                " Person@Example.com ",
                "StrongPassword!123",
                "127.0.0.1",
                "Arovia test client"
        );

        ArgumentCaptor<RegistrationData> registrationCaptor =
                ArgumentCaptor.forClass(RegistrationData.class);

        ArgumentCaptor<EmailNotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);

        verify(registrationPort).createPendingAccount(
                registrationCaptor.capture(),
                notificationCaptor.capture(),
                eq("127.0.0.1"),
                eq("Arovia test client")
        );

        RegistrationData registration = registrationCaptor.getValue();

        assertEquals("Person Example", registration.displayName());
        assertEquals("Person@Example.com", registration.email());
        assertEquals("person@example.com", registration.normalizedEmail());
        assertEquals("encoded-password", registration.passwordHash());

        assertEquals(
                "verification-token-hash",
                registration.verificationTokenHash()
        );

        assertEquals(
                NOW.plus(Duration.ofHours(24)),
                registration.verificationTokenExpiresAt()
        );

        EmailNotificationRequest notification = notificationCaptor.getValue();

        assertEquals(
                EmailNotificationType.EMAIL_VERIFICATION,
                notification.type()
        );

        assertEquals("Person@Example.com", notification.recipientEmail());
        assertEquals("Person Example", notification.recipientName());
        assertEquals("verification-token", notification.actionToken());
    }

    private IdentityRegistrationService createService() {
        return new IdentityRegistrationService(
                registrationPort,
                requestRateLimiter,
                passwordPolicy,
                passwordEncoder,
                tokenProvider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofHours(24)
        );
    }
}