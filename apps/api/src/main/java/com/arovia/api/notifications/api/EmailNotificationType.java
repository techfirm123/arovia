package com.arovia.api.notifications.api;

/**
 * Supported email notification types exposed by the Notifications module.
 *
 * The event values are persisted in the existing email_outbox table.
 * Keep these values stable unless a database migration and dispatcher
 * compatibility change are introduced.
 */
public enum EmailNotificationType {

    EMAIL_VERIFICATION("USER_EMAIL_VERIFICATION_REQUESTED"),

    PASSWORD_RESET("USER_PASSWORD_RESET_REQUESTED");

    private final String eventType;

    EmailNotificationType(String eventType) {
        this.eventType = eventType;
    }

    public String eventType() {
        return eventType;
    }
}