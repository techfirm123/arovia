package com.arovia.api.identity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IdentityAuditEventTypeTest {

    @Test
    void preservesExistingPersistedEventIdentifiers() {
        assertEquals(
                "USER_REGISTRATION_ACCEPTED",
                IdentityAuditEventType.USER_REGISTRATION_ACCEPTED.value()
        );

        assertEquals(
                "USER_EMAIL_VERIFICATION_RESEND_REQUESTED",
                IdentityAuditEventType
                        .USER_EMAIL_VERIFICATION_RESEND_REQUESTED
                        .value()
        );

        assertEquals(
                "USER_EMAIL_VERIFIED",
                IdentityAuditEventType.USER_EMAIL_VERIFIED.value()
        );

        assertEquals(
                "USER_PASSWORD_RESET_REQUESTED",
                IdentityAuditEventType.USER_PASSWORD_RESET_REQUESTED.value()
        );

        assertEquals(
                "USER_PASSWORD_RESET_COMPLETED",
                IdentityAuditEventType.USER_PASSWORD_RESET_COMPLETED.value()
        );

        assertEquals(
                "USER_LOGIN_RATE_LIMITED",
                IdentityAuditEventType.USER_LOGIN_RATE_LIMITED.value()
        );

        assertEquals(
                "USER_LOGIN_SUCCEEDED",
                IdentityAuditEventType.USER_LOGIN_SUCCEEDED.value()
        );

        assertEquals(
                "USER_LOGIN_FAILED",
                IdentityAuditEventType.USER_LOGIN_FAILED.value()
        );

        assertEquals(
                "USER_LOGOUT_SUCCEEDED",
                IdentityAuditEventType.USER_LOGOUT_SUCCEEDED.value()
        );
    }
}