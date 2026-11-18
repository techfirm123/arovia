package com.arovia.api.notifications.api;

import java.util.UUID;

/**
 * Public application contract for notification outbox operations.
 *
 * Callers should invoke these methods from their existing database
 * transaction. Implementations enforce this requirement.
 */
public interface NotificationOutbox {

    /**
     * Encrypts and enqueues an email notification for the supplied
     * aggregate inside the caller's current transaction.
     */
    void enqueue(
            UUID aggregateId,
            EmailNotificationRequest request
    );

    /**
     * Marks pending or retrying notifications of the given type for
     * the aggregate as superseded.
     *
     * Processing messages cannot safely be cancelled after an SMTP
     * worker has claimed them.
     */
    void supersedePending(
            EmailNotificationType type,
            UUID aggregateId
    );
}