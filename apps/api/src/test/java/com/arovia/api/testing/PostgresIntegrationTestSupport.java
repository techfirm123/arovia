package com.arovia.api.testing;

import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Shared configuration for PostgreSQL-backed integration tests.
 *
 * Integration tests are restricted to the dedicated arovia_test database.
 */
public final class PostgresIntegrationTestSupport {

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

    private PostgresIntegrationTestSupport() {
    }

    public static void registerProperties(
            DynamicPropertyRegistry registry,
            String encryptionKeyVersion
    ) {
        registry.add(
                "spring.datasource.url",
                PostgresIntegrationTestSupport::testDatabaseUrl
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
                PostgresIntegrationTestSupport::testDatabasePassword
        );

        registry.add(
                "arovia.outbox.encryption.key",
                () -> TEST_ENCRYPTION_KEY
        );

        registry.add(
                "arovia.outbox.encryption.key-version",
                () -> encryptionKeyVersion
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