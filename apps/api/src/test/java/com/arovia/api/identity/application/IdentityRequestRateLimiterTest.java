
package com.arovia.api.identity.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.arovia.api.identity.infrastructure.IdentityRateLimitStore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityRequestRateLimiterTest {

    private static final Duration ONE_HOUR = Duration.ofHours(1);

    @Mock
    private IdentityRateLimitStore rateLimitStore;

    @Test
    void allowsRegistrationWhenBothLimitsAllowRequest() {
        when(rateLimitStore.consume(
                "REGISTRATION_IP",
                "127.0.0.1",
                ONE_HOUR,
                10
        )).thenReturn(true);

        when(rateLimitStore.consume(
                "REGISTRATION_EMAIL",
                "user@example.com",
                ONE_HOUR,
                3
        )).thenReturn(true);

        IdentityRequestRateLimiter limiter = createLimiter();

        assertTrue(
                limiter.allowRegistration(
                        "127.0.0.1",
                        "user@example.com"
                )
        );
    }

    @Test
    void blocksRegistrationWhenIpLimitIsExceeded() {
        when(rateLimitStore.consume(
                "REGISTRATION_IP",
                "127.0.0.1",
                ONE_HOUR,
                10
        )).thenReturn(false);

        when(rateLimitStore.consume(
                "REGISTRATION_EMAIL",
                "user@example.com",
                ONE_HOUR,
                3
        )).thenReturn(true);

        IdentityRequestRateLimiter limiter = createLimiter();

        assertFalse(
                limiter.allowRegistration(
                        "127.0.0.1",
                        "user@example.com"
                )
        );
    }

    @Test
    void blocksRegistrationWhenEmailLimitIsExceeded() {
        when(rateLimitStore.consume(
                "REGISTRATION_IP",
                "127.0.0.1",
                ONE_HOUR,
                10
        )).thenReturn(true);

        when(rateLimitStore.consume(
                "REGISTRATION_EMAIL",
                "user@example.com",
                ONE_HOUR,
                3
        )).thenReturn(false);

        IdentityRequestRateLimiter limiter = createLimiter();

        assertFalse(
                limiter.allowRegistration(
                        "127.0.0.1",
                        "user@example.com"
                )
        );
    }

    @Test
    void appliesSeparateResendLimits() {
        when(rateLimitStore.consume(
                "VERIFICATION_RESEND_IP",
                "127.0.0.1",
                ONE_HOUR,
                20
        )).thenReturn(true);

        when(rateLimitStore.consume(
                "VERIFICATION_RESEND_EMAIL",
                "user@example.com",
                ONE_HOUR,
                5
        )).thenReturn(true);

        IdentityRequestRateLimiter limiter = createLimiter();

        assertTrue(
                limiter.allowResend(
                        "127.0.0.1",
                        "user@example.com"
                )
        );

        verify(rateLimitStore).consume(
                "VERIFICATION_RESEND_IP",
                "127.0.0.1",
                ONE_HOUR,
                20
        );

        verify(rateLimitStore).consume(
                "VERIFICATION_RESEND_EMAIL",
                "user@example.com",
                ONE_HOUR,
                5
        );
    }

    private IdentityRequestRateLimiter createLimiter() {
        return new IdentityRequestRateLimiter(
                rateLimitStore,
                ONE_HOUR,
                10,
                3,
                ONE_HOUR,
                20,
                5
        );
    }
}
