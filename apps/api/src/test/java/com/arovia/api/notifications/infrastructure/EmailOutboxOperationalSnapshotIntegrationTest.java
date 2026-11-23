package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.arovia.api.testing.PostgresIntegrationTestSupport;
import com.arovia.api.notifications.domain.EmailOutboxOperationalSnapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "arovia.scheduling.enabled=false",
                "spring.main.web-application-type=none"
        }
)
@Tag("integration")
class EmailOutboxOperationalSnapshotIntegrationTest {

    private static final String TEST_KEY_VERSION =
            "outbox-snapshot-integration-v1";

    private final List<UUID> insertedIds = new ArrayList<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailOutboxStore outboxStore;

    @DynamicPropertySource
    static void registerIntegrationTestProperties(
            DynamicPropertyRegistry registry
    ) {
        PostgresIntegrationTestSupport.registerProperties(
                registry,
                TEST_KEY_VERSION
        );
    }

    @BeforeEach
    void resetInsertedIds() {
        insertedIds.clear();
    }

    @AfterEach
    void removeTestRecords() {
        for (UUID id : insertedIds) {
            jdbcTemplate.update(
                    "DELETE FROM email_outbox WHERE id = ?",
                    id
            );
        }
    }

    @Test
    void calculatesOutboxOperationalSnapshotFromPostgres() {
        EmailOutboxOperationalSnapshot before =
                outboxStore.loadOperationalSnapshot();

        Instant now = Instant.now();

        UUID pendingId = insertMessage(
                "PENDING",
                true,
                now.minusSeconds(60),
                null,
                null,
                null
        );

        UUID retryId = insertMessage(
                "RETRY",
                true,
                now.minusSeconds(120),
                null,
                "ENCRYPTION_KEY_UNAVAILABLE",
                null
        );

        UUID processingId = insertMessage(
                "PROCESSING",
                true,
                now.minusSeconds(180),
                now.minusSeconds(30),
                null,
                null
        );

        UUID deadLetterId = insertMessage(
                "DEAD_LETTER",
                true,
                now.minusSeconds(3_600),
                null,
                "SMTP_DELIVERY_FAILED",
                null
        );

        UUID sentId = insertMessage(
                "SENT",
                false,
                now.minusSeconds(7_200),
                null,
                null,
                now
        );

        /*
         * Keep references alive in this method to make the fixture
         * records and their expected lifecycle states explicit.
         */
        assertTrue(List.of(
                pendingId,
                retryId,
                processingId,
                deadLetterId,
                sentId
        ).stream().distinct().count() == 5);

        EmailOutboxOperationalSnapshot after =
                outboxStore.loadOperationalSnapshot();

        assertEquals(before.pendingCount() + 1, after.pendingCount());
        assertEquals(before.retryCount() + 1, after.retryCount());
        assertEquals(before.processingCount() + 1, after.processingCount());
        assertEquals(before.deadLetterCount() + 1, after.deadLetterCount());

        assertEquals(
                before.retainedPayloadCount() + 4,
                after.retainedPayloadCount()
        );

        assertEquals(
                before.deadLetterRetainedPayloadCount() + 1,
                after.deadLetterRetainedPayloadCount()
        );

        assertEquals(
                before.encryptionKeyUnavailableCount() + 1,
                after.encryptionKeyUnavailableCount()
        );

        assertEquals(
                before.expiredProcessingCount() + 1,
                after.expiredProcessingCount()
        );

        assertTrue(
                after.oldestPendingAgeSeconds() >= 170.0,
                "Oldest active outbox message should be at least "
                        + "approximately 170 seconds old."
        );
    }

    private UUID insertMessage(
            String status,
            boolean retainPayload,
            Instant createdAt,
            Instant lockedUntil,
            String errorCode,
            Instant sentAt
    ) {
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        byte[] ciphertext = retainPayload
                ? new byte[]{1, 2, 3, 4}
                : null;

        byte[] nonce = retainPayload
                ? new byte[12]
                : null;

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
                    attempt_count,
                    next_attempt_at,
                    locked_until,
                    last_error_code,
                    created_at,
                    sent_at
                )
                VALUES (
                    ?, 'USER_EMAIL_VERIFICATION_REQUESTED',
                    'SNAPSHOT_TEST', ?, ?, ?, ?,
                    ?, 0, CURRENT_TIMESTAMP, ?, ?, ?, ?
                )
                """,
                id,
                aggregateId,
                ciphertext,
                nonce,
                TEST_KEY_VERSION,
                status,
                lockedUntil == null
                        ? null
                        : Timestamp.from(lockedUntil),
                errorCode,
                Timestamp.from(createdAt),
                sentAt == null
                        ? null
                        : Timestamp.from(sentAt)
        );

        insertedIds.add(id);

        return id;
    }
}