
package com.arovia.api.notifications.domain;

public record EmailVerificationEmailPayload(
        String recipientEmail,
        String displayName,
        String verificationToken
) {
}
