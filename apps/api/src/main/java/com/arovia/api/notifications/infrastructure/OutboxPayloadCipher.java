package com.arovia.api.notifications.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.arovia.api.configuration.AroviaProperties;
import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;
import com.arovia.api.notifications.domain.PasswordResetEmailPayload;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

@Component
public class OutboxPayloadCipher {

    private static final String TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final String KEY_ALGORITHM = "AES";

    private static final int NONCE_LENGTH_BYTES = 12;

    private static final int AUTHENTICATION_TAG_BITS = 128;

    private static final int AUTHENTICATION_TAG_LENGTH_BYTES = 16;

    private static final int AES_KEY_LENGTH_BYTES = 32;

    private static final Pattern KEY_VERSION_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,79}");

    private final JsonMapper jsonMapper;
    private final SecureRandom secureRandom;

    private final SecretKeySpec activeSecretKey;
    private final String activeKeyVersion;

    private final Map<String, SecretKeySpec> keysByVersion;

    public OutboxPayloadCipher(
            JsonMapper jsonMapper,
            SecureRandom secureRandom,
            AroviaProperties properties
    ) {
        this.jsonMapper = jsonMapper;
        this.secureRandom = secureRandom;

        AroviaProperties.Outbox.Encryption encryption =
                properties.getOutbox().getEncryption();

        this.activeKeyVersion = validateKeyVersion(
                encryption.getKeyVersion()
        );

        this.keysByVersion = createKeyRing(encryption);

        this.activeSecretKey = keysByVersion.get(activeKeyVersion);
    }

    public EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            EmailVerificationEmailPayload payload
    ) {
        return encryptPayload(outboxId, eventType, payload);
    }

    public EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            PasswordResetEmailPayload payload
    ) {
        return encryptPayload(outboxId, eventType, payload);
    }

    private EncryptedOutboxPayload encryptPayload(
            UUID outboxId,
            String eventType,
            Object payload
    ) {
        try {
            byte[] plaintext = jsonMapper.writeValueAsBytes(payload);

            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    activeSecretKey,
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_BITS,
                            nonce
                    )
            );

            cipher.updateAAD(
                    createAssociatedData(outboxId, eventType)
            );

            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptedOutboxPayload(
                    ciphertext,
                    nonce,
                    activeKeyVersion
            );

        } catch (
                GeneralSecurityException | JacksonException exception
        ) {
            throw new IllegalStateException(
                    "Unable to encrypt the email outbox payload.",
                    exception
            );
        }
    }

    public EmailVerificationEmailPayload decrypt(
            UUID outboxId,
            String eventType,
            EncryptedOutboxPayload encryptedPayload
    ) {
        return decryptPayload(
                outboxId,
                eventType,
                encryptedPayload,
                EmailVerificationEmailPayload.class
        );
    }

    public PasswordResetEmailPayload decryptPasswordReset(
            UUID outboxId,
            String eventType,
            EncryptedOutboxPayload encryptedPayload
    ) {
        return decryptPayload(
                outboxId,
                eventType,
                encryptedPayload,
                PasswordResetEmailPayload.class
        );
    }

    private <T> T decryptPayload(
            UUID outboxId,
            String eventType,
            EncryptedOutboxPayload encryptedPayload,
            Class<T> payloadType
    ) {
        if (encryptedPayload == null) {
            throw new IllegalArgumentException(
                    "Encrypted outbox payload is required."
            );
        }

        String requestedKeyVersion = encryptedPayload.keyVersion();

        SecretKeySpec decryptionKey =
                keysByVersion.get(requestedKeyVersion);

        if (decryptionKey == null) {
            throw new OutboxEncryptionKeyUnavailableException(
                    requestedKeyVersion
            );
        }

        validateEncryptedPayload(encryptedPayload);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    decryptionKey,
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_BITS,
                            encryptedPayload.nonce()
                    )
            );

            cipher.updateAAD(
                    createAssociatedData(outboxId, eventType)
            );

            byte[] plaintext = cipher.doFinal(
                    encryptedPayload.ciphertext()
            );

            return jsonMapper.readValue(
                    plaintext,
                    payloadType
            );

        } catch (
                GeneralSecurityException | JacksonException exception
        ) {
            throw new IllegalStateException(
                    "Unable to decrypt the email outbox payload.",
                    exception
            );
        }
    }

    private Map<String, SecretKeySpec> createKeyRing(
            AroviaProperties.Outbox.Encryption encryption
    ) {
        Map<String, SecretKeySpec> keyRing = new HashMap<>();

        List<byte[]> loadedKeyMaterial = new ArrayList<>();

        byte[] activeKeyBytes = decodeKey(
                activeKeyVersion,
                encryption.getKey()
        );

        keyRing.put(
                activeKeyVersion,
                new SecretKeySpec(activeKeyBytes, KEY_ALGORITHM)
        );

        loadedKeyMaterial.add(activeKeyBytes);

        Map<String, String> additionalKeys =
                encryption.getAdditionalKeys();

        if (additionalKeys == null) {
            throw new IllegalStateException(
                    "Additional outbox encryption keys cannot be null."
            );
        }

        for (Map.Entry<String, String> entry
                : additionalKeys.entrySet()) {

            String version = validateKeyVersion(entry.getKey());

            if (keyRing.containsKey(version)) {
                throw new IllegalStateException(
                        "Outbox encryption key versions must be unique."
                );
            }

            byte[] keyBytes = decodeKey(
                    version,
                    entry.getValue()
            );

            ensureUniqueKeyMaterial(
                    keyBytes,
                    loadedKeyMaterial
            );

            keyRing.put(
                    version,
                    new SecretKeySpec(keyBytes, KEY_ALGORITHM)
            );

            loadedKeyMaterial.add(keyBytes);
        }

        return Map.copyOf(keyRing);
    }

    private void ensureUniqueKeyMaterial(
            byte[] candidate,
            List<byte[]> loadedKeys
    ) {
        for (byte[] existing : loadedKeys) {
            if (MessageDigest.isEqual(existing, candidate)) {
                throw new IllegalStateException(
                        "Arovia outbox encryption keys must use "
                                + "distinct key material."
                );
            }
        }
    }

    private byte[] decodeKey(
            String version,
            String base64Key
    ) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "Arovia outbox encryption key is not configured "
                            + "for version: " + version
            );
        }

        try {
            byte[] key = Base64.getDecoder().decode(base64Key.trim());

            if (key.length != AES_KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "Arovia outbox encryption key must contain "
                                + "32 bytes. Version: " + version
                );
            }

            return key;

        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Arovia outbox encryption key must be valid "
                            + "Base64. Version: " + version,
                    exception
            );
        }
    }

    private String validateKeyVersion(String version) {
        if (version == null
                || !KEY_VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalStateException(
                    "Arovia outbox encryption key version is invalid."
            );
        }

        return version;
    }

    private void validateEncryptedPayload(
            EncryptedOutboxPayload encryptedPayload
    ) {
        byte[] nonce = encryptedPayload.nonce();
        byte[] ciphertext = encryptedPayload.ciphertext();

        if (nonce == null || nonce.length != NONCE_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "Outbox encryption nonce must contain 12 bytes."
            );
        }

        if (ciphertext == null
                || ciphertext.length < AUTHENTICATION_TAG_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "Outbox encrypted payload is invalid."
            );
        }
    }

    private byte[] createAssociatedData(
            UUID outboxId,
            String eventType
    ) {
        String value = outboxId + ":" + eventType;

        return value.getBytes(StandardCharsets.UTF_8);
    }
}