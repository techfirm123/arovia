package com.arovia.api.notifications.api;

import java.util.Objects;

/**
 * Public request contract for enqueueing an email notification.
 *
 * The action token is sensitive. Implementations must encrypt the
 * notification payload before persistence and must never log this object.
 */
public record EmailNotificationRequest(
        EmailNotificationType type,
        String recipientEmail,
        String recipientName,
        String actionToken
) {

    public EmailNotificationRequest {
        Objects.requireNonNull(type, "Notification type is required.");

        requireNonBlank(recipientEmail, "Recipient email is required.");
        requireNonBlank(recipientName, "Recipient name is required.");
        requireNonBlank(actionToken, "Notification action token is required.");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}