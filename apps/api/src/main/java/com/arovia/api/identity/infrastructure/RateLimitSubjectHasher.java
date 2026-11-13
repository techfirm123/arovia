
package com.arovia.api.identity.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RateLimitSubjectHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec secretKey;

    public RateLimitSubjectHasher(
            @Value("${arovia.rate-limit.hmac-key:}") String base64Key
    ) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "Arovia rate-limit HMAC key is not configured."
            );
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());

            if (keyBytes.length != KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "Arovia rate-limit HMAC key must contain 32 bytes."
                );
            }

            this.secretKey = new SecretKeySpec(
                    keyBytes,
                    HMAC_ALGORITHM
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Arovia rate-limit HMAC key must be valid Base64.",
                    exception
            );
        }
    }

    public String hash(String action, String subject) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Rate-limit action is required."
            );
        }

        String normalizedSubject =
                subject == null || subject.isBlank()
                        ? "unknown"
                        : subject.strip().toLowerCase(Locale.ROOT);

        String value = action + "\0" + normalizedSubject;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);

            byte[] digest = mac.doFinal(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(digest);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to hash the rate-limit subject.",
                    exception
            );
        }
    }
}
