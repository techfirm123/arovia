package com.arovia.api.identity.application.port;

import com.arovia.api.identity.domain.RegistrationData;
import com.arovia.api.notifications.api.EmailNotificationRequest;

/**
 * Outbound persistence boundary for the identity registration use case.
 *
 * <p>The implementation is responsible for persisting the pending account,
 * verification token, notification outbox message and audit event as one
 * transaction.</p>
 */
public interface IdentityRegistrationPort {

    void createPendingAccount(
            RegistrationData registration,
            EmailNotificationRequest notificationRequest,
            String sourceIp,
            String userAgent
    );
}