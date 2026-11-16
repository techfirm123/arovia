package com.arovia.api.notifications.domain;

public record PasswordResetEmailPayload(
        String recipientEmail,
        String displayName,
        String resetToken
) {
}