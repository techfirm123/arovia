
package com.arovia.api.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsLongPassphraseWithoutCompositionRules() {
        String password = "correct horse battery staple";

        String result = passwordPolicy.validateAndNormalize(
                password,
                "user@example.com"
        );

        assertEquals(password, result);
    }

    @Test
    void rejectsPasswordShorterThanFifteenCharacters() {
        assertThrows(
                PasswordPolicyException.class,
                () -> passwordPolicy.validateAndNormalize(
                        "short-password",
                        "user@example.com"
                )
        );
    }

    @Test
    void rejectsPasswordLongerThanMaximumLength() {
        String password = "a".repeat(129);

        assertThrows(
                PasswordPolicyException.class,
                () -> passwordPolicy.validateAndNormalize(
                        password,
                        "user@example.com"
                )
        );
    }

    @Test
    void rejectsCommonPasswordFromLocalDenylist() {
        assertThrows(
                PasswordPolicyException.class,
                () -> passwordPolicy.validateAndNormalize(
                        "passwordpassword",
                        "user@example.com"
                )
        );
    }

    @Test
    void rejectsPasswordContainingEmailLocalPart() {
        assertThrows(
                PasswordPolicyException.class,
                () -> passwordPolicy.validateAndNormalize(
                        "alexander very secret passphrase",
                        "alexander@example.com"
                )
        );
    }

    @Test
    void normalizesUnicodePasswordToNfc() {
        String decomposed = "correct horse cafe\u0301 battery";

        String result = passwordPolicy.validateAndNormalize(
                decomposed,
                "user@example.com"
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                java.text.Normalizer.normalize(
                        decomposed,
                        java.text.Normalizer.Form.NFC
                ),
                result
        );
    }
}
