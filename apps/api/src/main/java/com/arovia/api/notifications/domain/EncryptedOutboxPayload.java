
package com.arovia.api.notifications.domain;

public record EncryptedOutboxPayload(
        byte[] ciphertext,
        byte[] nonce,
        String keyVersion
) {
}
