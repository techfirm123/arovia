
package com.arovia.api.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.arovia.api.identity.application.IdentityRegistrationService;
import com.arovia.api.identity.application.InvalidEmailVerificationTokenException;
import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.identity.domain.RegistrationData;
import com.arovia.api.identity.infrastructure.IdentityEmailVerificationResendStore;
import com.arovia.api.identity.infrastructure.IdentityEmailVerificationStore;
import com.arovia.api.identity.infrastructure.IdentityRateLimitStore;
import com.arovia.api.identity.infrastructure.IdentityRegistrationStore;
import com.arovia.api.identity.infrastructure.RateLimitSubjectHasher;
import com.arovia.api.notifications.domain.EncryptedOutboxPayload;

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
                "arovia.scheduling.enabled=false"
        }
)
@Tag("integration")
class IdentityPersistenceIntegrationTest {

    private static final String VERIFICATION_EVENT_TYPE =
            "USER_EMAIL_VERIFICATION_REQUESTED";

    private static final String INTEGRATION_KEY_VERSION =
            "integration-test-v1";

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

    private static final Duration RESEND_COOLDOWN =
            Duration.ofSeconds(60);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityRegistrationStore registrationStore;

    @Autowired
    private IdentityRegistrationService registrationService;

    @Autowired
    private IdentityEmailVerificationResendStore resendStore;

    @Autowired
    private IdentityEmailVerificationStore verificationStore;

    @Autowired
    private IdentityRateLimitStore rateLimitStore;

    @Autowired
    private RateLimitSubjectHasher subjectHasher;

    @DynamicPropertySource
    static void registerIntegrationTestProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                IdentityPersistenceIntegrationTest::testDatabaseUrl
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
                IdentityPersistenceIntegrationTest::testDatabasePassword
        );

        registry.add(
                "arovia.outbox.encryption.key",
                () -> TEST_ENCRYPTION_KEY
        );

        registry.add(
                "arovia.outbox.encryption.key-version",
                () -> INTEGRATION_KEY_VERSION
        );

        registry.add(
                "arovia.rate-limit.hmac-key",
                () -> TEST_HMAC_KEY
        );

        registry.add(
                "arovia.scheduling.enabled",
                () -> "false"
        );

        registry.add(
                "spring.main.web-application-type",
                () -> "none"
        );
    }

    @BeforeEach
    void cleanIntegrationDatabase() {
        /*
         * This test suite is allowed to clear only the dedicated
         * arovia_test database configured above.
         */
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    identity_rate_limit_bucket,
                    identity_audit_event,
                    email_outbox,
                    email_verification_token,
                    user_account
                CASCADE
                """
        );
    }

    @Test
    void registrationRateLimitAllowsThreeAttemptsAndSuppressesFourth() {
        String email = "rate-limit@example.com";

        for (int attempt = 1; attempt <= 4; attempt++) {
            registrationService.register(
                    "Rate Limit Integration Test",
                    email,
                    "correct horse battery staple",
                    "192.0.2.10",
                    "Arovia integration test"
            );
        }

        assertEquals(
                1,
                count(
                        "SELECT COUNT(*) FROM user_account "
                                + "WHERE normalized_email = ?",
                        email
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token AS token
                        INNER JOIN user_account AS account
                            ON account.id = token.user_account_id
                        WHERE account.normalized_email = ?
                        """,
                        email
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_outbox AS outbox
                        INNER JOIN user_account AS account
                            ON account.id = outbox.aggregate_id
                        WHERE account.normalized_email = ?
                        """,
                        email
                )
        );

        String emailHash = subjectHasher.hash(
                "REGISTRATION_EMAIL",
                email
        );

        Long attempts = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(attempt_count), 0)
                FROM identity_rate_limit_bucket
                WHERE action = 'REGISTRATION_EMAIL'
                  AND subject_hash = ?
                """,
                Long.class,
                emailHash
        );

        assertEquals(4L, attempts);
    }

    @Test
    void concurrentRegistrationsCreateOnlyOneAccountAndRelatedRecords()
            throws Exception {

        int concurrentRequests = 8;
        String email = "concurrent-registration@example.com";

        ExecutorService executor = Executors.newFixedThreadPool(
                concurrentRequests
        );

        CountDownLatch ready = new CountDownLatch(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < concurrentRequests; index++) {
                RegistrationData registration =
                        createRegistrationData(email, validPayload());

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Concurrent registration start timed out."
                        );
                    }

                    registrationStore.createPendingAccount(
                            registration,
                            "192.0.2.20",
                            "Arovia integration test"
                    );

                    return null;
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "All registration tasks should be ready."
            );

            start.countDown();

            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertEquals(
                1,
                count(
                        "SELECT COUNT(*) FROM user_account "
                                + "WHERE normalized_email = ?",
                        email
                )
        );

        assertEquals(
                1,
                count("SELECT COUNT(*) FROM email_verification_token")
        );

        assertEquals(
                1,
                count("SELECT COUNT(*) FROM email_outbox")
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM identity_audit_event
                        WHERE event_type = 'USER_REGISTRATION_ACCEPTED'
                        """
                )
        );
    }

    @Test
    void registrationRollsBackAccountAndTokenWhenOutboxPersistenceFails() {
        String email = "registration-rollback@example.com";

        RegistrationData invalidRegistration =
                createRegistrationData(email, null);

        assertThrows(
                NullPointerException.class,
                () -> registrationStore.createPendingAccount(
                        invalidRegistration,
                        "192.0.2.30",
                        "Arovia integration test"
                )
        );

        assertEquals(
                0,
                count(
                        "SELECT COUNT(*) FROM user_account "
                                + "WHERE normalized_email = ?",
                        email
                )
        );

        assertEquals(
                0,
                count("SELECT COUNT(*) FROM email_verification_token")
        );

        assertEquals(
                0,
                count("SELECT COUNT(*) FROM email_outbox")
        );

        assertEquals(
                0,
                count("SELECT COUNT(*) FROM identity_audit_event")
        );
    }

    @Test
    void concurrentResendsAllowOnlyOneReplacementWithinCooldown()
            throws Exception {

        String email = "concurrent-resend@example.com";

        RegistrationData initialRegistration = createPendingAccount(email);

        backdateResendTimestamp(email);

        int concurrentRequests = 8;

        ExecutorService executor = Executors.newFixedThreadPool(
                concurrentRequests
        );

        CountDownLatch ready = new CountDownLatch(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < concurrentRequests; index++) {
                EmailVerificationResendData resendData =
                        createResendData();

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Concurrent resend start timed out."
                        );
                    }

                    return resendStore.resendIfEligible(
                            email,
                            resendData,
                            RESEND_COOLDOWN,
                            "192.0.2.40",
                            "Arovia integration test"
                    );
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "All resend tasks should be ready."
            );

            start.countDown();

            int successfulResends = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successfulResends++;
                }
            }

            assertEquals(1, successfulResends);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token
                        WHERE user_account_id = ?
                          AND consumed_at IS NULL
                          AND revoked_at IS NULL
                        """,
                        initialRegistration.userAccountId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token
                        WHERE user_account_id = ?
                          AND revoked_at IS NOT NULL
                        """,
                        initialRegistration.userAccountId()
                )
        );

        assertEquals(
                2,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_outbox
                        WHERE aggregate_id = ?
                        """,
                        initialRegistration.userAccountId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM identity_audit_event
                        WHERE user_account_id = ?
                          AND event_type =
                              'USER_EMAIL_VERIFICATION_RESEND_REQUESTED'
                        """,
                        initialRegistration.userAccountId()
                )
        );
    }

    @Test
    void resendRollsBackTokenRevocationAndOutboxChangesOnFailure() {
        String email = "resend-rollback@example.com";

        RegistrationData initialRegistration = createPendingAccount(email);

        backdateResendTimestamp(email);

        EmailVerificationResendData invalidResend =
                createResendDataWithMissingPayload();

        assertThrows(
                NullPointerException.class,
                () -> resendStore.resendIfEligible(
                        email,
                        invalidResend,
                        RESEND_COOLDOWN,
                        "192.0.2.50",
                        "Arovia integration test"
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token
                        WHERE user_account_id = ?
                          AND consumed_at IS NULL
                          AND revoked_at IS NULL
                        """,
                        initialRegistration.userAccountId()
                )
        );

        assertEquals(
                0,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token
                        WHERE user_account_id = ?
                          AND revoked_at IS NOT NULL
                        """,
                        initialRegistration.userAccountId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_outbox
                        WHERE aggregate_id = ?
                          AND status = 'PENDING'
                          AND payload_ciphertext IS NOT NULL
                        """,
                        initialRegistration.userAccountId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token
                        WHERE user_account_id = ?
                        """,
                        initialRegistration.userAccountId()
                )
        );
    }

    @Test
    void concurrentVerificationConsumesTokenAndActivatesAccountOnlyOnce()
            throws Exception {

        String email = "concurrent-verification@example.com";

        RegistrationData registration = createPendingAccount(email);

        int concurrentRequests = 2;

        ExecutorService executor = Executors.newFixedThreadPool(
                concurrentRequests
        );

        CountDownLatch ready = new CountDownLatch(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < concurrentRequests; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Concurrent verification start timed out."
                        );
                    }

                    try {
                        verificationStore.activateAccount(
                                registration.verificationTokenHash(),
                                "192.0.2.60",
                                "Arovia integration test"
                        );

                        return true;
                    } catch (InvalidEmailVerificationTokenException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "All verification tasks should be ready."
            );

            start.countDown();

            int successfulConfirmations = 0;
            int rejectedConfirmations = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successfulConfirmations++;
                } else {
                    rejectedConfirmations++;
                }
            }

            assertEquals(1, successfulConfirmations);
            assertEquals(1, rejectedConfirmations);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM user_account
                        WHERE normalized_email = ?
                          AND status = 'ACTIVE'
                          AND email_verified_at IS NOT NULL
                        """,
                        email
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM email_verification_token
                        WHERE user_account_id = ?
                          AND consumed_at IS NOT NULL
                        """,
                        registration.userAccountId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM identity_audit_event
                        WHERE user_account_id = ?
                          AND event_type = 'USER_EMAIL_VERIFIED'
                        """,
                        registration.userAccountId()
                )
        );
    }

    @Test
    void rateLimitStoreRejectsFourthAttemptAndPersistsCounter() {
        String action = "INTEGRATION_TEST_RATE_LIMIT";
        String subject = "integration-test@example.com";

        boolean first = rateLimitStore.consume(
                action,
                subject,
                Duration.ofHours(1),
                3
        );

        boolean second = rateLimitStore.consume(
                action,
                subject,
                Duration.ofHours(1),
                3
        );

        boolean third = rateLimitStore.consume(
                action,
                subject,
                Duration.ofHours(1),
                3
        );

        boolean fourth = rateLimitStore.consume(
                action,
                subject,
                Duration.ofHours(1),
                3
        );

        assertTrue(first);
        assertTrue(second);
        assertTrue(third);
        assertFalse(fourth);

        String subjectHash = subjectHasher.hash(action, subject);

        Long attempts = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(attempt_count), 0)
                FROM identity_rate_limit_bucket
                WHERE action = ?
                  AND subject_hash = ?
                """,
                Long.class,
                action,
                subjectHash
        );

        assertEquals(4L, attempts);
    }

    private RegistrationData createPendingAccount(String email) {
        RegistrationData registration =
                createRegistrationData(email, validPayload());

        registrationStore.createPendingAccount(
                registration,
                "192.0.2.70",
                "Arovia integration test"
        );

        return registration;
    }

    private RegistrationData createRegistrationData(
            String email,
            EncryptedOutboxPayload payload
    ) {
        UUID userAccountId = UUID.randomUUID();

        return new RegistrationData(
                userAccountId,
                email,
                email.toLowerCase(),
                "integration-test-password-hash",
                "Arovia Integration Test",
                UUID.randomUUID(),
                hash(UUID.randomUUID().toString()),
                Instant.now().plus(Duration.ofHours(24)),
                UUID.randomUUID(),
                VERIFICATION_EVENT_TYPE,
                payload
        );
    }

    private EmailVerificationResendData createResendData() {
        return new EmailVerificationResendData(
                UUID.randomUUID(),
                hash(UUID.randomUUID().toString()),
                Instant.now().plus(Duration.ofHours(24)),
                UUID.randomUUID(),
                VERIFICATION_EVENT_TYPE,
                validPayload()
        );
    }

    private EmailVerificationResendData createResendDataWithMissingPayload() {
        return new EmailVerificationResendData(
                UUID.randomUUID(),
                hash(UUID.randomUUID().toString()),
                Instant.now().plus(Duration.ofHours(24)),
                UUID.randomUUID(),
                VERIFICATION_EVENT_TYPE,
                null
        );
    }

    private EncryptedOutboxPayload validPayload() {
        return new EncryptedOutboxPayload(
                new byte[]{1, 2, 3, 4},
                new byte[12],
                INTEGRATION_KEY_VERSION
        );
    }

    private void backdateResendTimestamp(String email) {
        jdbcTemplate.update(
                """
                UPDATE user_account
                SET verification_email_last_requested_at =
                    CURRENT_TIMESTAMP - INTERVAL '2 minutes'
                WHERE normalized_email = ?
                """,
                email
        );
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                arguments
        );

        return result == null ? 0 : result;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }

    private static String testDatabaseUrl() {
        String url = requiredEnvironmentVariable("AROVIA_TEST_DB_URL");

        String urlWithoutQuery = url.split("\\?", 2)[0];
        String databaseName = urlWithoutQuery.substring(
                urlWithoutQuery.lastIndexOf('/') + 1
        );

        if (!"arovia_test".equals(databaseName)) {
            throw new IllegalStateException(
                    "Integration tests may only use the arovia_test database. "
                            + "Configured database: " + databaseName
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
