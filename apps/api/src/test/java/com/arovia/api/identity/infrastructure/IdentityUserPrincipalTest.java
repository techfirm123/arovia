package com.arovia.api.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class IdentityUserPrincipalTest {

    @Test
    void activeVerifiedAccountIsEnabled() {
        IdentityUserPrincipal principal = new IdentityUserPrincipal(
                UUID.randomUUID(),
                "user@example.com",
                "user@example.com",
                "Sample User",
                "argon2-password-hash",
                "ACTIVE",
                true
        );

        assertTrue(principal.isEnabled());
        assertEquals("user@example.com", principal.getUsername());
        assertEquals("user@example.com", principal.getEmail());
    }

    @Test
    void pendingAccountIsNotEnabled() {
        IdentityUserPrincipal principal = new IdentityUserPrincipal(
                UUID.randomUUID(),
                "user@example.com",
                "user@example.com",
                "Sample User",
                "argon2-password-hash",
                "PENDING_EMAIL_VERIFICATION",
                false
        );

        assertFalse(principal.isEnabled());
    }

    @Test
    void suspendedAccountIsNotEnabled() {
        IdentityUserPrincipal principal = new IdentityUserPrincipal(
                UUID.randomUUID(),
                "user@example.com",
                "user@example.com",
                "Sample User",
                "argon2-password-hash",
                "SUSPENDED",
                true
        );

        assertFalse(principal.isEnabled());
    }

    @Test
    void credentialsAreErasedAfterAuthentication() {
        IdentityUserPrincipal principal = new IdentityUserPrincipal(
                UUID.randomUUID(),
                "user@example.com",
                "user@example.com",
                "Sample User",
                "argon2-password-hash",
                "ACTIVE",
                true
        );

        assertEquals("argon2-password-hash", principal.getPassword());

        principal.eraseCredentials();

        assertNull(principal.getPassword());
    }
}