package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import com.arovia.api.configuration.AroviaProperties;
import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

class OutboxPayloadCipherTest {

    private static final String EVENT_TYPE =
            EmailNotificationType.EMAIL_VERIFICATION.eventType();

    private static final String KEY_VERSION = "test-v1";

    private final String base64Key = encodeKey((byte) 0);

    private OutboxPayloadCipher createCipher() {
        return createCipher(
                base64Key,
                KEY_VERSION,
                Map.of()
        );
    }

    private OutboxPayloadCipher createCipher(
            String activeKey,
            String activeKeyVersion,
            Map<String, String> additionalKeys
    ) {
        AroviaProperties properties = new AroviaProperties();

        AroviaProperties.Outbox.Encryption encryption =
                properties.getOutbox().getEncryption();

        encryption.setKey(activeKey);
        encryption.setKeyVersion(activeKeyVersion);
        encryption.setAdditionalKeys(additionalKeys);

        return new OutboxPayloadCipher(
                JsonMapper.builder().build(),
                new SecureRandom(),
                properties
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
    void decryptsPayloadEncryptedWithAdditionalKeyAfterRotation() {
        String oldKey = encodeKey((byte) 1);
        String newKey = encodeKey((byte) 2);

        UUID outboxId = UUID.randomUUID();

        EmailVerificationEmailPayload original =
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "rotation-test-token"
                );

        OutboxPayloadCipher oldCipher = createCipher(
                oldKey,
                "test-v1",
                Map.of()
        );

        EncryptedOutboxPayload encryptedWithOldKey =
                oldCipher.encrypt(
                        outboxId,
                        EVENT_TYPE,
                        original
                );

        OutboxPayloadCipher rotatedCipher = createCipher(
                newKey,
                "test-v2",
                Map.of("test-v1", oldKey)
        );

        EmailVerificationEmailPayload decrypted =
                rotatedCipher.decrypt(
                        outboxId,
                        EVENT_TYPE,
                        encryptedWithOldKey
                );

        assertEquals(original, decrypted);
    }

    @Test
    void encryptsNewPayloadsUsingActiveKeyAfterRotation() {
        String oldKey = encodeKey((byte) 1);
        String newKey = encodeKey((byte) 2);

        OutboxPayloadCipher rotatedCipher = createCipher(
                newKey,
                "test-v2",
                Map.of("test-v1", oldKey)
        );

        UUID outboxId = UUID.randomUUID();

        EmailVerificationEmailPayload original =
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "new-key-token"
                );

        EncryptedOutboxPayload encrypted = rotatedCipher.encrypt(
                outboxId,
                EVENT_TYPE,
                original
        );

        assertEquals("test-v2", encrypted.keyVersion());

        assertEquals(
                original,
                rotatedCipher.decrypt(
                        outboxId,
                        EVENT_TYPE,
                        encrypted
                )
        );
    }

    @Test
    void reportsUnavailableKeyVersionWithoutFallback() {
        String oldKey = encodeKey((byte) 1);
        String newKey = encodeKey((byte) 2);

        OutboxPayloadCipher oldCipher = createCipher(
                oldKey,
                "test-v1",
                Map.of()
        );

        EncryptedOutboxPayload oldPayload = oldCipher.encrypt(
                UUID.randomUUID(),
                EVENT_TYPE,
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "old-key-token"
                )
        );

        OutboxPayloadCipher newCipher = createCipher(
                newKey,
                "test-v2",
                Map.of()
        );

        UUID outboxId = UUID.randomUUID();

        EncryptedOutboxPayload encrypted = oldCipher.encrypt(
                outboxId,
                EVENT_TYPE,
                new EmailVerificationEmailPayload(
                        "user@example.com",
                        "Sample User",
                        "old-key-token"
                )
        );

        assertThrows(
                OutboxEncryptionKeyUnavailableException.class,
                () -> newCipher.decrypt(
                        outboxId,
                        EVENT_TYPE,
                        encrypted
                )
        );
    }

    @Test
    void rejectsDuplicateKeyMaterialAcrossVersions() {
        String sameKey = encodeKey((byte) 1);

        assertThrows(
                IllegalStateException.class,
                () -> createCipher(
                        sameKey,
                        "test-v2",
                        Map.of("test-v1", sameKey)
                )
        );
    }

    @Test
    void rejectsInvalidAdditionalEncryptionKeyLength() {
        String invalidKey = Base64.getEncoder().encodeToString(
                new byte[16]
        );

        assertThrows(
                IllegalStateException.class,
                () -> createCipher(
                        encodeKey((byte) 2),
                        "test-v2",
                        Map.of("test-v1", invalidKey)
                )
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
    void rejectsInvalidActiveEncryptionKeyLength() {
        AroviaProperties properties = new AroviaProperties();

        properties.getOutbox()
                .getEncryption()
                .setKey(
                        Base64.getEncoder().encodeToString(new byte[16])
                );

        assertThrows(
                IllegalStateException.class,
                () -> new OutboxPayloadCipher(
                        JsonMapper.builder().build(),
                        new SecureRandom(),
                        properties
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

    private String encodeKey(byte value) {
        byte[] key = new byte[32];
        Arrays.fill(key, value);

        return Base64.getEncoder().encodeToString(key);
    }
}