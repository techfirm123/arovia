
package com.arovia.api.notifications.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.notifications.domain.ClaimedOutboxMessage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EmailOutboxStore {

    private final JdbcTemplate jdbcTemplate;

    public EmailOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                    status = 'SENT',
                    sent_at = CURRENT_TIMESTAMP,
                    locked_until = NULL,
                    payload_ciphertext = NULL,
                    payload_nonce = NULL,
                    last_error_code = NULL
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                outboxId
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
                    status = 'RETRY',
                    next_attempt_at = ?,
                    locked_until = NULL,
                    last_error_code = ?
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                Timestamp.from(nextAttemptAt),
                errorCode,
                outboxId
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
                    status = 'DEAD_LETTER',
                    locked_until = NULL,
                    last_error_code = ?
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                errorCode,
                outboxId
        );

        requireSingleUpdate(updated, outboxId);
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
