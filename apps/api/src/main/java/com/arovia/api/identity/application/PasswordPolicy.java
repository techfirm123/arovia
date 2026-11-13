
package com.arovia.api.identity.application;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private static final int MINIMUM_CODE_POINTS = 15;
    private static final int MAXIMUM_CODE_POINTS = 128;

    /*
     * Initial local denylist.
     *
     * This is a small seed list for the first implementation.
     * A comprehensive compromised-password corpus/checker must
     * be added before public launch.
     */
    private static final Set<String> BLOCKED_PASSWORDS = Set.of(
            "passwordpassword",
            "password12345678",
            "123456789012345",
            "1234567890123456",
            "qwertyuiopasdfgh",
            "letmeinletmein",
            "adminadminadmin",
            "welcome12345678",
            "iloveyouiloveyou",
            "aroviaarovia123",
            "computercomputer",
            "changemechangeme"
    );

    public String validateAndNormalize(
            String rawPassword,
            String normalizedEmail
    ) {
        if (rawPassword == null) {
            throw new PasswordPolicyException();
        }

        String normalizedPassword = Normalizer.normalize(
                rawPassword,
                Normalizer.Form.NFC
        );

        int codePointCount = normalizedPassword.codePointCount(
                0,
                normalizedPassword.length()
        );

        if (codePointCount < MINIMUM_CODE_POINTS
                || codePointCount > MAXIMUM_CODE_POINTS) {
            throw new PasswordPolicyException();
        }

        String comparisonValue = normalizedPassword.toLowerCase(Locale.ROOT);

        if (BLOCKED_PASSWORDS.contains(comparisonValue)) {
            throw new PasswordPolicyException();
        }

        rejectEmailDerivedPassword(comparisonValue, normalizedEmail);

        return normalizedPassword;
    }

    private void rejectEmailDerivedPassword(
            String normalizedPassword,
            String normalizedEmail
    ) {
        if (normalizedEmail == null) {
            return;
        }

        int separator = normalizedEmail.indexOf('@');

        if (separator <= 0) {
            return;
        }

        String emailLocalPart = normalizedEmail
                .substring(0, separator)
                .toLowerCase(Locale.ROOT);

        if (emailLocalPart.length() >= 4
                && normalizedPassword.contains(emailLocalPart)) {
            throw new PasswordPolicyException();
        }
    }
}
