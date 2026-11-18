package com.arovia.api.identity.application.port;

import java.time.Duration;

import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.notifications.api.EmailNotificationRequest;

public interface IdentityEmailVerificationResendPort {

    boolean resendIfEligible(
            String normalizedEmail,
            EmailVerificationResendData resendData,
            EmailNotificationRequest notificationRequest,
            Duration resendCooldown,
            String sourceIp,
            String userAgent
    );
}