package com.arovia.api.notifications.domain;

/**
 * Persisted lifecycle states for an email outbox record.
 *
 * Values must remain compatible with the email_outbox.status column.
 */
public enum EmailOutboxStatus {

    PENDING("PENDING"),
    RETRY("RETRY"),
    PROCESSING("PROCESSING"),
    SENT("SENT"),
    DEAD_LETTER("DEAD_LETTER");

    private final String value;

    EmailOutboxStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}