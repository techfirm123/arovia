
package com.arovia.api.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdentityRateLimitCleanupJob {

    private static final Logger log =
            LoggerFactory.getLogger(IdentityRateLimitCleanupJob.class);

    private final IdentityRateLimitStore rateLimitStore;

    public IdentityRateLimitCleanupJob(
            IdentityRateLimitStore rateLimitStore
    ) {
        this.rateLimitStore = rateLimitStore;
    }

    @Scheduled(
            fixedDelayString =
                    "${arovia.rate-limit.cleanup-interval-ms:900000}"
    )
    public void cleanupExpiredBuckets() {
        int deleted = rateLimitStore.purgeExpiredBatch();

        if (deleted > 0) {
            log.debug(
                    "Removed expired identity rate-limit buckets: {}",
                    deleted
            );
        }
    }
}
