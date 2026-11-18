package com.arovia.api.notifications.domain;

public record EmailOutboxOperationalSnapshot(
        long pendingCount,
        long retryCount,
        long processingCount,
        long deadLetterCount,
        long retainedPayloadCount,
        long deadLetterRetainedPayloadCount,
        long encryptionKeyUnavailableCount,
        long expiredProcessingCount,
        double oldestPendingAgeSeconds
) {

    public static EmailOutboxOperationalSnapshot empty() {
        return new EmailOutboxOperationalSnapshot(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0.0
        );
    }
}