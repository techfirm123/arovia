package com.arovia.api.security.session;

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

class SecuritySessionPropertiesTest {

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
    void shouldUseTwelveHoursAsDefaultAbsoluteTimeout() {
        SecuritySessionProperties properties =
                new SecuritySessionProperties();

        assertEquals(
                Duration.ofHours(12),
                properties.getAbsoluteTimeout()
        );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void shouldRejectZeroAbsoluteTimeout() {
        SecuritySessionProperties properties =
                new SecuritySessionProperties();

        properties.setAbsoluteTimeout(Duration.ZERO);

        Set<ConstraintViolation<SecuritySessionProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectNegativeAbsoluteTimeout() {
        SecuritySessionProperties properties =
                new SecuritySessionProperties();

        properties.setAbsoluteTimeout(Duration.ofMinutes(-1));

        Set<ConstraintViolation<SecuritySessionProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectNullAbsoluteTimeout() {
        SecuritySessionProperties properties =
                new SecuritySessionProperties();

        properties.setAbsoluteTimeout(null);

        Set<ConstraintViolation<SecuritySessionProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }
}