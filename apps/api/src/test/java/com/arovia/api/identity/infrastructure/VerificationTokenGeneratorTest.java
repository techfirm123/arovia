
package com.arovia.api.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

class VerificationTokenGeneratorTest {

    private final VerificationTokenGenerator generator =
            new VerificationTokenGenerator(new SecureRandom());

    @Test
    void generatesUrlSafeToken() {
        String token = generator.generateToken();

        assertNotNull(token);
        assertEquals(43, token.length());
        org.junit.jupiter.api.Assertions.assertTrue(
                token.matches("[A-Za-z0-9_-]+")
        );
    }

    @Test
    void generatesDifferentTokens() {
        String first = generator.generateToken();
        String second = generator.generateToken();

        assertNotEquals(first, second);
    }

    @Test
    void storesOnlyAHashRepresentation() {
        String token = generator.generateToken();

        String hash = generator.hashToken(token);

        assertEquals(64, hash.length());
        assertNotEquals(token, hash);
    }
}
