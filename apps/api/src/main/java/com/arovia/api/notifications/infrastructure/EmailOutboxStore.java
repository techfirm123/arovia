package com.arovia.api.notifications.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.ClaimedOutboxMessage;
import com.arovia.api.notifications.domain.EmailOutboxOperationalSnapshot;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EmailOutboxStore {

    private static final String AGGREGATE_TYPE_USER_ACCOUNT =
            "USER_ACCOUNT";

    private static final String PENDING_STATUS = "PENDING";

    private static final String RETRY_STATUS = "RETRY";

    private static final String PROCESSING_STATUS = "PROCESSING";

    private static final String SENT_STATUS = "SENT";

    private static final String DEAD_LETTER_STATUS = "DEAD_LETTER";

    private static final String SUPERSEDED_ERROR_CODE =
            "SUPERSEDED_BY_RESEND";

    private final JdbcTemplate jdbcTemplate;

    public EmailOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persists an already encrypted notification.
     *
     * This operation must participate in an existing transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(
            UUID outboxId,
            EmailNotificationType type,
            UUID aggregateId,
            EncryptedOutboxPayload encryptedPayload
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO email_outbox (
                    id,
                    event_type,
                    aggregate_type,
                    aggregate_id,
                    payload_ciphertext,
                    payload_nonce,
                    encryption_key_version,
                    status,
                    attempt_count
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                outboxId,
                type.eventType(),
                AGGREGATE_TYPE_USER_ACCOUNT,
                aggregateId,
                encryptedPayload.ciphertext(),
                encryptedPayload.nonce(),
                encryptedPayload.keyVersion(),
                PENDING_STATUS
        );
    }

    /**
     * Supersedes messages that have not yet been claimed by a dispatcher.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void supersedePending(
            EmailNotificationType type,
            UUID aggregateId
    ) {
        jdbcTemplate.update(
                """
                UPDATE email_outbox
                SET
                    status = ?,
                    payload_ciphertext = NULL,
                    payload_nonce = NULL,
                    locked_until = NULL,
                    last_error_code = ?
                WHERE aggregate_type = ?
                  AND aggregate_id = ?
                  AND event_type = ?
                  AND status IN (?, ?)
                """,
                DEAD_LETTER_STATUS,
                SUPERSEDED_ERROR_CODE,
                AGGREGATE_TYPE_USER_ACCOUNT,
                aggregateId,
                type.eventType(),
                PENDING_STATUS,
                RETRY_STATUS
        );
    }

    /*
     * This method runs in its own transaction.
     * The transaction commits before the dispatcher performs SMTP delivery.
     */
    @Transactional
    public Optional<ClaimedOutboxMessage> claimNext() {
        List<ClaimedOutboxMessage> messages = jdbcTemplate.query(
                """
                WITH candidate AS (
                    SELECT id
                    FROM email_outbox
                    WHERE
                        status = 'PENDING'
                        OR (
                            status = 'RETRY'
                            AND next_attempt_at <= CURRENT_TIMESTAMP
                        )
                        OR (
                            status = 'PROCESSING'
                            AND locked_until < CURRENT_TIMESTAMP
                        )
                    ORDER BY next_attempt_at, created_at
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE email_outbox AS outbox
                SET
                    status = 'PROCESSING',
                    attempt_count = outbox.attempt_count + 1,
                    locked_until = CURRENT_TIMESTAMP + INTERVAL '2 minutes',
                    last_error_code = NULL
                FROM candidate
                WHERE outbox.id = candidate.id
                RETURNING
                    outbox.id,
                    outbox.event_type,
                    outbox.payload_ciphertext,
                    outbox.payload_nonce,
                    outbox.encryption_key_version,
                    outbox.attempt_count
                """,
                (resultSet, rowNumber) -> new ClaimedOutboxMessage(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("event_type"),
                        resultSet.getBytes("payload_ciphertext"),
                        resultSet.getBytes("payload_nonce"),
                        resultSet.getString("encryption_key_version"),
                        resultSet.getInt("attempt_count")
                )
        );

        return messages.stream().findFirst();
    }

    @Transactional
    public void markSent(UUID outboxId) {
        int updated = jdbcTemplate.update(
                """
                UPDATE email_outbox
                SET
                    status = ?,
                    sent_at = CURRENT_TIMESTAMP,
                    locked_until = NULL,
                    payload_ciphertext = NULL,
                    payload_nonce = NULL,
                    last_error_code = NULL
                WHERE id = ?
                  AND status = ?
                """,
                SENT_STATUS,
                outboxId,
                PROCESSING_STATUS
        );

        requireSingleUpdate(updated, outboxId);
    }

    @Transactional
    public void scheduleRetry(
            UUID outboxId,
            Instant nextAttemptAt,
            String errorCode
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE email_outbox
                SET
                    status = ?,
                    next_attempt_at = ?,
                    locked_until = NULL,
                    last_error_code = ?
                WHERE id = ?
                  AND status = ?
                """,
                RETRY_STATUS,
                Timestamp.from(nextAttemptAt),
                errorCode,
                outboxId,
                PROCESSING_STATUS
        );

        requireSingleUpdate(updated, outboxId);
    }

    /**
     * Schedules another attempt without consuming the SMTP delivery budget.
     */
    @Transactional
    public void scheduleRetryWithoutConsumingAttempt(
            UUID outboxId,
            Instant nextAttemptAt,
            String errorCode
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE email_outbox
                SET
                    status = ?,
                    next_attempt_at = ?,
                    locked_until = NULL,
                    last_error_code = ?,
                    attempt_count = GREATEST(attempt_count - 1, 0)
                WHERE id = ?
                  AND status = ?
                """,
                RETRY_STATUS,
                Timestamp.from(nextAttemptAt),
                errorCode,
                outboxId,
                PROCESSING_STATUS
        );

        requireSingleUpdate(updated, outboxId);
    }

    @Transactional
    public void markDeadLetter(
            UUID outboxId,
            String errorCode
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE email_outbox
                SET
                    status = ?,
                    locked_until = NULL,
                    last_error_code = ?
                WHERE id = ?
                  AND status = ?
                """,
                DEAD_LETTER_STATUS,
                errorCode,
                outboxId,
                PROCESSING_STATUS
        );

        requireSingleUpdate(updated, outboxId);
    }

    /**
     * Reads a single aggregate snapshot for operational metrics.
     *
     * This query does not retrieve ciphertext, nonce, email address,
     * notification payloads or tokens.
     */
    @Transactional(readOnly = true)
    public EmailOutboxOperationalSnapshot loadOperationalSnapshot() {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    pending_count,
                    retry_count,
                    processing_count,
                    dead_letter_count,
                    retained_payload_count,
                    dead_letter_retained_payload_count,
                    encryption_key_unavailable_count,
                    expired_processing_count,

                    COALESCE(
                        GREATEST(
                            EXTRACT(
                                EPOCH FROM (
                                    CURRENT_TIMESTAMP
                                    - oldest_active_created_at
                                )
                            ),
                            0
                        ),
                        0
                    )::double precision AS oldest_pending_age_seconds

                FROM (
                    SELECT
                        COUNT(*) FILTER (
                            WHERE status = 'PENDING'
                        ) AS pending_count,

                        COUNT(*) FILTER (
                            WHERE status = 'RETRY'
                        ) AS retry_count,

                        COUNT(*) FILTER (
                            WHERE status = 'PROCESSING'
                        ) AS processing_count,

                        COUNT(*) FILTER (
                            WHERE status = 'DEAD_LETTER'
                        ) AS dead_letter_count,

                        COUNT(*) FILTER (
                            WHERE payload_ciphertext IS NOT NULL
                        ) AS retained_payload_count,

                        COUNT(*) FILTER (
                            WHERE payload_ciphertext IS NOT NULL
                              AND status = 'DEAD_LETTER'
                        ) AS dead_letter_retained_payload_count,

                        COUNT(*) FILTER (
                            WHERE payload_ciphertext IS NOT NULL
                              AND last_error_code =
                                  'ENCRYPTION_KEY_UNAVAILABLE'
                        ) AS encryption_key_unavailable_count,

                        COUNT(*) FILTER (
                            WHERE status = 'PROCESSING'
                              AND (
                                  locked_until IS NULL
                                  OR locked_until < CURRENT_TIMESTAMP
                              )
                        ) AS expired_processing_count,

                        MIN(created_at) FILTER (
                            WHERE status IN (
                                'PENDING',
                                'RETRY',
                                'PROCESSING'
                            )
                        ) AS oldest_active_created_at

                    FROM email_outbox
                ) AS outbox_snapshot
                """,
                (resultSet, rowNumber) ->
                        new EmailOutboxOperationalSnapshot(
                                resultSet.getLong("pending_count"),
                                resultSet.getLong("retry_count"),
                                resultSet.getLong("processing_count"),
                                resultSet.getLong("dead_letter_count"),
                                resultSet.getLong("retained_payload_count"),
                                resultSet.getLong(
                                        "dead_letter_retained_payload_count"
                                ),
                                resultSet.getLong(
                                        "encryption_key_unavailable_count"
                                ),
                                resultSet.getLong("expired_processing_count"),
                                resultSet.getDouble(
                                        "oldest_pending_age_seconds"
                                )
                        )
        );
    }

    private void requireSingleUpdate(
            int updated,
            UUID outboxId
    ) {
        if (updated != 1) {
            throw new IllegalStateException(
                    "Expected one processing outbox record to update: "
                            + outboxId
            );
        }
    }
}