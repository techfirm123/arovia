package com.arovia.api.identity.application.port;

import java.time.Duration;

public interface IdentityRateLimitPort {

    boolean consume(
            String action,
            String subject,
            Duration window,
            int maximumAttempts
    );
}