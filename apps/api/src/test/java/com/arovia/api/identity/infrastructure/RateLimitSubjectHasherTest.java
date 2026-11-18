package com.arovia.api.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;

import com.arovia.api.identity.configuration.IdentityRateLimitProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitSubjectHasherTest {

    private static final String KEY =
            Base64.getEncoder().encodeToString(new byte[32]);

    private RateLimitSubjectHasher hasher;

    @BeforeEach
    void setUp() {
        IdentityRateLimitProperties properties =
                new IdentityRateLimitProperties();

        properties.setHmacKey(KEY);

        hasher = new RateLimitSubjectHasher(properties);
    }

    @Test
    void producesStableDigestForSameActionAndSubject() {
        String first = hasher.hash(
                "REGISTRATION_EMAIL",
                "user@example.com"
        );

        String second = hasher.hash(
                "REGISTRATION_EMAIL",
                "user@example.com"
        );

        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void differentActionsProduceDifferentDigests() {
        String registration = hasher.hash(
                "REGISTRATION_EMAIL",
                "user@example.com"
        );

        String resend = hasher.hash(
                "VERIFICATION_RESEND_EMAIL",
                "user@example.com"
        );

        assertNotEquals(registration, resend);
    }

    @Test
    void normalizesSubjectBeforeHashing() {
        String first = hasher.hash(
                "REGISTRATION_EMAIL",
                "User@Example.com"
        );

        String second = hasher.hash(
                "REGISTRATION_EMAIL",
                " user@example.com "
        );

        assertEquals(first, second);
    }

    @Test
    void digestDoesNotContainRawSubject() {
        String digest = hasher.hash(
                "REGISTRATION_EMAIL",
                "user@example.com"
        );

        assertFalse(digest.contains("user@example.com"));
    }
}