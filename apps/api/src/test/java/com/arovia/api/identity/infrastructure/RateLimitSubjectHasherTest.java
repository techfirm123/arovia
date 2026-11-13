
package com.arovia.api.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class RateLimitSubjectHasherTest {

    private static final String KEY =
            Base64.getEncoder().encodeToString(new byte[32]);

    private final RateLimitSubjectHasher hasher =
            new RateLimitSubjectHasher(KEY);

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
