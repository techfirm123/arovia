
package com.arovia.api.notifications.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.arovia.api.notifications.domain.EmailVerificationEmailPayload;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxPayloadCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int AUTHENTICATION_TAG_BITS = 128;
    private static final int AES_KEY_LENGTH_BYTES = 32;

    private final JsonMapper jsonMapper;
    private final SecureRandom secureRandom;
    private final String base64Key;
    private final String keyVersion;

    public OutboxPayloadCipher(
            JsonMapper jsonMapper,
            SecureRandom secureRandom,
            @Value("${arovia.outbox.encryption.key:}") String base64Key,
            @Value("${arovia.outbox.encryption.key-version:local-v1}")
            String keyVersion
    ) {
        this.jsonMapper = jsonMapper;
        this.secureRandom = secureRandom;
        this.base64Key = base64Key;
        this.keyVersion = keyVersion;
    }

    public EncryptedOutboxPayload encrypt(
            UUID outboxId,
            String eventType,
            EmailVerificationEmailPayload payload
    ) {
        try {
            byte[] plaintext = jsonMapper.writeValueAsBytes(payload);

            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(readKey(), "AES"),
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_BITS,
                            nonce
                    )
            );

            cipher.updateAAD(createAssociatedData(outboxId, eventType));

            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptedOutboxPayload(
                    ciphertext,
                    nonce,
                    keyVersion
            );
        } catch (GeneralSecurityException | JacksonException exception) {
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
        try {
            if (!keyVersion.equals(encryptedPayload.keyVersion())) {
                throw new IllegalStateException(
                        "Outbox encryption key version is not available."
                );
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(readKey(), "AES"),
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_BITS,
                            encryptedPayload.nonce()
                    )
            );

            cipher.updateAAD(createAssociatedData(outboxId, eventType));

            byte[] plaintext = cipher.doFinal(
                    encryptedPayload.ciphertext()
            );

            return jsonMapper.readValue(
                    plaintext,
                    EmailVerificationEmailPayload.class
            );
        } catch (GeneralSecurityException | JacksonException exception) {
            throw new IllegalStateException(
                    "Unable to decrypt the email outbox payload.",
                    exception
            );
        }
    }

    private byte[] readKey() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "Arovia outbox encryption key is not configured."
            );
        }

        try {
            byte[] key = Base64.getDecoder().decode(base64Key.trim());

            if (key.length != AES_KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "Arovia outbox encryption key must contain 32 bytes."
                );
            }

            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Arovia outbox encryption key must be valid Base64.",
                    exception
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
