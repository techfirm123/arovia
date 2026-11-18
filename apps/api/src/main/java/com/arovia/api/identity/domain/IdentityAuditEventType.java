package com.arovia.api.identity.domain;

/**
 * Stable event identifiers persisted in identity_audit_event.event_type.
 *
 * These values are part of the persisted audit contract. Do not rename
 * them without considering existing audit records and downstream consumers.
 */
public enum IdentityAuditEventType {

    USER_REGISTRATION_ACCEPTED("USER_REGISTRATION_ACCEPTED"),

    USER_EMAIL_VERIFICATION_RESEND_REQUESTED(
            "USER_EMAIL_VERIFICATION_RESEND_REQUESTED"
    ),

    USER_EMAIL_VERIFIED("USER_EMAIL_VERIFIED"),

    USER_PASSWORD_RESET_REQUESTED("USER_PASSWORD_RESET_REQUESTED"),

    USER_PASSWORD_RESET_COMPLETED("USER_PASSWORD_RESET_COMPLETED"),

    USER_LOGIN_RATE_LIMITED("USER_LOGIN_RATE_LIMITED"),

    USER_LOGIN_SUCCEEDED("USER_LOGIN_SUCCEEDED"),

    USER_LOGIN_FAILED("USER_LOGIN_FAILED"),

    USER_LOGOUT_SUCCEEDED("USER_LOGOUT_SUCCEEDED");

    private final String value;

    IdentityAuditEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}