package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

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

    private static final String TEST_ENCRYPTION_KEY =
            Base64.getEncoder().encodeToString(new byte[32]);

    private static final String TEST_HMAC_KEY =
            Base64.getEncoder().encodeToString(
                    new byte[]{
                            1, 2, 3, 4, 5, 6, 7, 8,
                            9, 10, 11, 12, 13, 14, 15, 16,
                            17, 18, 19, 20, 21, 22, 23, 24,
                            25, 26, 27, 28, 29, 30, 31, 32
                    }
            );

    private final List<UUID> insertedIds = new ArrayList<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailOutboxStore outboxStore;

    @DynamicPropertySource
    static void registerIntegrationTestProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                EmailOutboxOperationalSnapshotIntegrationTest::testDatabaseUrl
        );

        registry.add(
                "spring.datasource.username",
                () -> environmentOrDefault(
                        "AROVIA_TEST_DB_USERNAME",
                        "arovia_app"
                )
        );

        registry.add(
                "spring.datasource.password",
                EmailOutboxOperationalSnapshotIntegrationTest
                        ::testDatabasePassword
        );

        registry.add(
                "arovia.outbox.encryption.key",
                () -> TEST_ENCRYPTION_KEY
        );

        registry.add(
                "arovia.outbox.encryption.key-version",
                () -> TEST_KEY_VERSION
        );

        registry.add(
                "arovia.rate-limit.hmac-key",
                () -> TEST_HMAC_KEY
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

    private static String testDatabaseUrl() {
        String url = requiredEnvironmentVariable("AROVIA_TEST_DB_URL");

        String urlWithoutQuery = url.split("\\?", 2)[0];

        String databaseName = urlWithoutQuery.substring(
                urlWithoutQuery.lastIndexOf('/') + 1
        );

        if (!"arovia_test".equals(databaseName)) {
            throw new IllegalStateException(
                    "Integration tests may only use the arovia_test "
                            + "database. Configured database: "
                            + databaseName
            );
        }

        return url;
    }

    private static String testDatabasePassword() {
        String password = System.getenv("AROVIA_TEST_DB_PASSWORD");

        if (password == null || password.isBlank()) {
            password = System.getenv("AROVIA_DB_PASSWORD");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Set AROVIA_TEST_DB_PASSWORD or AROVIA_DB_PASSWORD "
                            + "for PostgreSQL integration tests."
            );
        }

        return password;
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required integration-test environment variable "
                            + name + " is missing."
            );
        }

        return value;
    }

    private static String environmentOrDefault(
            String name,
            String defaultValue
    ) {
        String value = System.getenv(name);

        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }
}