package com.arovia.api.configuration;

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

class IdentityWorkflowPropertiesTest {

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
    void exposesExpectedDefaultDurations() {
        IdentityWorkflowProperties properties =
                new IdentityWorkflowProperties();

        assertEquals(
                Duration.ofHours(24),
                properties.getEmailVerificationTtl()
        );

        assertEquals(
                Duration.ofMinutes(1),
                properties.getEmailVerificationResendCooldown()
        );

        assertEquals(
                Duration.ofMinutes(30),
                properties.getPasswordResetTtl()
        );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsZeroPasswordResetTtl() {
        IdentityWorkflowProperties properties =
                new IdentityWorkflowProperties();

        properties.setPasswordResetTtl(Duration.ZERO);

        Set<ConstraintViolation<IdentityWorkflowProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsNegativeVerificationTtl() {
        IdentityWorkflowProperties properties =
                new IdentityWorkflowProperties();

        properties.setEmailVerificationTtl(Duration.ofMinutes(-1));

        Set<ConstraintViolation<IdentityWorkflowProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }
}