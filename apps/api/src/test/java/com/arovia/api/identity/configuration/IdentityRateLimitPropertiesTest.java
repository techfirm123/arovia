package com.arovia.api.identity.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityRateLimitPropertiesTest {

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
    void exposesExpectedDefaultPolicies() {
        IdentityRateLimitProperties properties =
                new IdentityRateLimitProperties();

        properties.setHmacKey("test-hmac-key");

        assertEquals(
                Duration.ofHours(1),
                properties.getRegistration().getWindow()
        );
        assertEquals(10, properties.getRegistration().getIpLimit());
        assertEquals(3, properties.getRegistration().getEmailLimit());

        assertEquals(
                Duration.ofHours(1),
                properties.getResend().getWindow()
        );
        assertEquals(20, properties.getResend().getIpLimit());
        assertEquals(5, properties.getResend().getEmailLimit());

        assertEquals(
                Duration.ofMinutes(15),
                properties.getLogin().getWindow()
        );
        assertEquals(60, properties.getLogin().getIpLimit());
        assertEquals(10, properties.getLogin().getEmailLimit());

        assertEquals(
                Duration.ofHours(1),
                properties.getPasswordReset().getWindow()
        );
        assertEquals(20, properties.getPasswordReset().getIpLimit());
        assertEquals(5, properties.getPasswordReset().getEmailLimit());
        assertEquals(
                10,
                properties.getPasswordReset().getConfirmationLimit()
        );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsMissingHmacKey() {
        IdentityRateLimitProperties properties =
                new IdentityRateLimitProperties();

        Set<ConstraintViolation<IdentityRateLimitProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsNonPositiveRegistrationWindow() {
        IdentityRateLimitProperties properties =
                new IdentityRateLimitProperties();

        properties.setHmacKey("test-hmac-key");
        properties.getRegistration().setWindow(Duration.ZERO);

        Set<ConstraintViolation<IdentityRateLimitProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsZeroEmailLimit() {
        IdentityRateLimitProperties properties =
                new IdentityRateLimitProperties();

        properties.setHmacKey("test-hmac-key");
        properties.getRegistration().setEmailLimit(0);

        Set<ConstraintViolation<IdentityRateLimitProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }
}