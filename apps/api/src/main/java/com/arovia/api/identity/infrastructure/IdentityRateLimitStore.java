
package com.arovia.api.identity.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityRateLimitStore {

    private final JdbcTemplate jdbcTemplate;
    private final RateLimitSubjectHasher subjectHasher;
    private final Clock clock;

    public IdentityRateLimitStore(
            JdbcTemplate jdbcTemplate,
            RateLimitSubjectHasher subjectHasher,
            Clock applicationClock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.subjectHasher = subjectHasher;
        this.clock = applicationClock;
    }

    @Transactional
    public boolean consume(
            String action,
            String subject,
            Duration window,
            int maximumAttempts
    ) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "Rate-limit window must be positive."
            );
        }

        if (maximumAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be positive."
            );
        }

        long windowSeconds = window.toSeconds();

        if (windowSeconds < 1) {
            throw new IllegalArgumentException(
                    "Rate-limit window must be at least one second."
            );
        }

        long currentEpochSecond = clock.instant().getEpochSecond();

        long bucketStartEpoch = Math.floorDiv(
                currentEpochSecond,
                windowSeconds
        ) * windowSeconds;

        Instant windowStart = Instant.ofEpochSecond(bucketStartEpoch);

        Instant expiresAt = windowStart
                .plus(window)
                .plus(window);

        String subjectHash = subjectHasher.hash(action, subject);

        Integer attemptCount = jdbcTemplate.queryForObject(
                """
                INSERT INTO identity_rate_limit_bucket (
                    action,
                    subject_hash,
                    window_start,
                    attempt_count,
                    expires_at
                )
                VALUES (?, ?, ?, 1, ?)
                ON CONFLICT (
                    action,
                    subject_hash,
                    window_start
                )
                DO UPDATE SET
                    attempt_count =
                        identity_rate_limit_bucket.attempt_count + 1,
                    expires_at = EXCLUDED.expires_at,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING attempt_count
                """,
                Integer.class,
                action,
                subjectHash,
                Timestamp.from(windowStart),
                Timestamp.from(expiresAt)
        );

        return attemptCount != null
                && attemptCount <= maximumAttempts;
    }

    @Transactional
    public int purgeExpiredBatch() {
        return jdbcTemplate.update(
                """
                WITH expired AS (
                    SELECT
                        action,
                        subject_hash,
                        window_start
                    FROM identity_rate_limit_bucket
                    WHERE expires_at < CURRENT_TIMESTAMP
                    ORDER BY expires_at
                    LIMIT 1000
                )
                DELETE FROM identity_rate_limit_bucket AS bucket
                USING expired
                WHERE bucket.action = expired.action
                  AND bucket.subject_hash = expired.subject_hash
                  AND bucket.window_start = expired.window_start
                """
        );
    }
}
