
package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

class OutboxPayloadCipherTest {

    private static final String EVENT_TYPE =
            "USER_EMAIL_VERIFICATION_REQUESTED";

    private static final String KEY_VERSION = "test-v1";

    private final String base64Key = Base64.getEncoder().encodeToString(
            new byte[32]
    );

    private OutboxPayloadCipher createCipher() {
        return new OutboxPayloadCipher(
                JsonMapper.builder().build(),
                new SecureRandom(),
                base64Key,
                KEY_VERSION
        );
    }

    @Test
    void encryptsAndDecryptsEmailPayload() {
        OutboxPayloadCipher cipher = createCipher();

        UUID outboxId = UUID.randomUUID();

        EmailVerificationEmailPayload original =
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "sensitive-verification-token"
                );

        EncryptedOutboxPayload encrypted = cipher.encrypt(
                outboxId,
                EVENT_TYPE,
                original
        );

        EmailVerificationEmailPayload decrypted = cipher.decrypt(
                outboxId,
                EVENT_TYPE,
                encrypted
        );

        assertEquals(original, decrypted);
    }

    @Test
    void encryptsSamePayloadWithDifferentNonces() {
        OutboxPayloadCipher cipher = createCipher();

        UUID outboxId = UUID.randomUUID();

        EmailVerificationEmailPayload payload =
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "sensitive-verification-token"
                );

        EncryptedOutboxPayload first = cipher.encrypt(
                outboxId,
                EVENT_TYPE,
                payload
        );

        EncryptedOutboxPayload second = cipher.encrypt(
                outboxId,
                EVENT_TYPE,
                payload
        );

        assertNotEquals(
                Base64.getEncoder().encodeToString(first.nonce()),
                Base64.getEncoder().encodeToString(second.nonce())
        );

        assertNotEquals(
                Base64.getEncoder().encodeToString(first.ciphertext()),
                Base64.getEncoder().encodeToString(second.ciphertext())
        );
    }

    @Test
    void rejectsTamperedCiphertext() {
        OutboxPayloadCipher cipher = createCipher();

        UUID outboxId = UUID.randomUUID();

        EncryptedOutboxPayload encrypted = cipher.encrypt(
                outboxId,
                EVENT_TYPE,
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "sensitive-verification-token"
                )
        );

        byte[] tamperedCiphertext = encrypted.ciphertext().clone();
        tamperedCiphertext[0] ^= 1;

        EncryptedOutboxPayload tampered = new EncryptedOutboxPayload(
                tamperedCiphertext,
                encrypted.nonce(),
                encrypted.keyVersion()
        );

        assertThrows(
                IllegalStateException.class,
                () -> cipher.decrypt(outboxId, EVENT_TYPE, tampered)
        );
    }

    @Test
    void rejectsPayloadWhenAssociatedEventIdentityChanges() {
        OutboxPayloadCipher cipher = createCipher();

        UUID outboxId = UUID.randomUUID();

        EncryptedOutboxPayload encrypted = cipher.encrypt(
                outboxId,
                EVENT_TYPE,
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "sensitive-verification-token"
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> cipher.decrypt(
                        outboxId,
                        "DIFFERENT_EVENT",
                        encrypted
                )
        );
    }

    @Test
    void rejectsDifferentOutboxIdentifier() {
        OutboxPayloadCipher cipher = createCipher();

        EncryptedOutboxPayload encrypted = cipher.encrypt(
                UUID.randomUUID(),
                EVENT_TYPE,
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "sensitive-verification-token"
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> cipher.decrypt(
                        UUID.randomUUID(),
                        EVENT_TYPE,
                        encrypted
                )
        );
    }

    @Test
    void nonceContainsTwelveBytes() {
        OutboxPayloadCipher cipher = createCipher();

        EncryptedOutboxPayload encrypted = cipher.encrypt(
                UUID.randomUUID(),
                EVENT_TYPE,
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "sensitive-verification-token"
                )
        );

        assertEquals(12, encrypted.nonce().length);
    }
}
