package com.arovia.api.notifications.domain;

/**
 * Stable error identifiers persisted in email_outbox.last_error_code.
 */
public enum EmailOutboxErrorCode {

    SUPERSEDED_BY_RESEND("SUPERSEDED_BY_RESEND"),
    UNSUPPORTED_EVENT_TYPE("UNSUPPORTED_EVENT_TYPE"),
    PAYLOAD_PROCESSING_FAILED("PAYLOAD_PROCESSING_FAILED"),
    SMTP_DELIVERY_FAILED("SMTP_DELIVERY_FAILED"),
    ENCRYPTION_KEY_UNAVAILABLE("ENCRYPTION_KEY_UNAVAILABLE");

    private final String value;

    EmailOutboxErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}