
package com.arovia.api.notifications.domain;

import java.util.UUID;

public record ClaimedOutboxMessage(
        UUID id,
        String eventType,
        byte[] ciphertext,
        byte[] nonce,
        String encryptionKeyVersion,
        int attemptCount
) {
}
