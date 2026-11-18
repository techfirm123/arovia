package com.arovia.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AroviaPropertiesTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void exposesExpectedOutboxDefaults() {
        AroviaProperties properties = validProperties();

        assertEquals(
                5_000,
                properties.getOutbox().getPollIntervalMs()
        );

        assertEquals(10, properties.getOutbox().getBatchSize());

        assertTrue(
                properties.getOutbox()
                        .getEncryption()
                        .getAdditionalKeys()
                        .isEmpty()
        );

        assertEquals(
                8,
                properties.getOutbox().getRetry().getMaxAttempts()
        );

        assertEquals(
                60,
                properties.getOutbox().getRetry()
                        .getBaseRetryDelaySeconds()
        );

        assertEquals(
                3_600,
                properties.getOutbox().getRetry()
                        .getMaxRetryDelaySeconds()
        );

        assertEquals(
                30,
                properties.getOutbox().getRetry().getMaxJitterSeconds()
        );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsMissingEncryptionKey() {
        AroviaProperties properties = new AroviaProperties();

        Set<ConstraintViolation<AroviaProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsNonPositiveBatchSize() {
        AroviaProperties properties = validProperties();
        properties.getOutbox().setBatchSize(0);

        Set<ConstraintViolation<AroviaProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsRetryBaseDelayGreaterThanMaximum() {
        AroviaProperties properties = validProperties();

        properties.getOutbox().getRetry()
                .setBaseRetryDelaySeconds(120);

        properties.getOutbox().getRetry()
                .setMaxRetryDelaySeconds(60);

        Set<ConstraintViolation<AroviaProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsNegativeJitter() {
        AroviaProperties properties = validProperties();

        properties.getOutbox().getRetry().setMaxJitterSeconds(-1);

        Set<ConstraintViolation<AroviaProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void acceptsValidAdditionalEncryptionKey() {
        AroviaProperties properties = validProperties();

        properties.getOutbox()
                .getEncryption()
                .getAdditionalKeys()
                .put(
                        "previous-v1",
                        Base64.getEncoder().encodeToString(
                                createKeyBytes((byte) 1)
                        )
                );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsAdditionalKeyUsingActiveKeyVersion() {
        AroviaProperties properties = validProperties();

        String activeVersion = properties.getOutbox()
                .getEncryption()
                .getKeyVersion();

        properties.getOutbox()
                .getEncryption()
                .getAdditionalKeys()
                .put(
                        activeVersion,
                        Base64.getEncoder().encodeToString(
                                createKeyBytes((byte) 1)
                        )
                );

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsBlankAdditionalEncryptionKey() {
        AroviaProperties properties = validProperties();

        properties.getOutbox()
                .getEncryption()
                .getAdditionalKeys()
                .put("previous-v1", " ");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsInvalidAdditionalKeyVersion() {
        AroviaProperties properties = validProperties();

        properties.getOutbox()
                .getEncryption()
                .getAdditionalKeys()
                .put(
                        "invalid version",
                        Base64.getEncoder().encodeToString(
                                createKeyBytes((byte) 1)
                        )
                );

        assertFalse(validator.validate(properties).isEmpty());
    }

    private AroviaProperties validProperties() {
        AroviaProperties properties = new AroviaProperties();

        properties.getOutbox()
                .getEncryption()
                .setKey(
                        Base64.getEncoder().encodeToString(
                                createKeyBytes((byte) 0)
                        )
                );

        return properties;
    }

    private byte[] createKeyBytes(byte value) {
        byte[] key = new byte[32];

        java.util.Arrays.fill(key, value);

        return key;
    }
}