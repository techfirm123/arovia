
package com.arovia.api.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class Argon2PasswordEncoderTest {

    @Test
    void hashesAndVerifiesPasswordUsingArgon2id() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
                16,
                32,
                1,
                19_456,
                2
        );

        String rawPassword = "correct horse battery staple";

        String encodedPassword = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertFalse(encodedPassword.contains(rawPassword));
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }
}
