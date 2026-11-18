package com.arovia.api.notifications.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.EmailOutboxOperationalSnapshot;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class EmailOutboxObservabilityTest {

    @Mock
    private EmailOutboxStore outboxStore;

    private SimpleMeterRegistry meterRegistry;

    private EmailOutboxObservability observability;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        observability = new EmailOutboxObservability(
                meterRegistry,
                outboxStore
        );
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    @Test
    void refreshesOperationalGaugesFromDatabaseSnapshot() {
        EmailOutboxOperationalSnapshot snapshot =
                new EmailOutboxOperationalSnapshot(
                        12,
                        5,
                        2,
                        3,
                        18,
                        2,
                        1,
                        1,
                        125.5
                );

        when(outboxStore.loadOperationalSnapshot())
                .thenReturn(snapshot);

        observability.refreshSnapshot();

        assertGauge(
                EmailOutboxObservability.PENDING_GAUGE,
                12
        );

        assertGauge(
                EmailOutboxObservability.RETRY_GAUGE,
                5
        );

        assertGauge(
                EmailOutboxObservability.PROCESSING_GAUGE,
                2
        );

        assertGauge(
                EmailOutboxObservability.DEAD_LETTER_GAUGE,
                3
        );

        assertGauge(
                EmailOutboxObservability.RETAINED_PAYLOAD_GAUGE,
                18
        );

        assertGauge(
                EmailOutboxObservability.DEAD_LETTER_RETAINED_PAYLOAD_GAUGE,
                2
        );

        assertGauge(
                EmailOutboxObservability.ENCRYPTION_KEY_UNAVAILABLE_GAUGE,
                1
        );

        assertGauge(
                EmailOutboxObservability.EXPIRED_PROCESSING_GAUGE,
                1
        );

        assertGauge(
                EmailOutboxObservability.OLDEST_PENDING_AGE_GAUGE,
                125.5
        );
    }

    @Test
    void recordsSuccessfulDispatchCounterAndDuration() {
        observability.recordDispatch(
                EmailNotificationType.EMAIL_VERIFICATION.eventType(),
                EmailOutboxObservability.OUTCOME_SENT,
                Duration.ofMillis(250)
        );

        double count = meterRegistry.get(
                        EmailOutboxObservability.DISPATCH_COUNTER
                )
                .tags(
                        "event_type",
                        "email_verification",
                        "outcome",
                        EmailOutboxObservability.OUTCOME_SENT
                )
                .counter()
                .count();

        assertEquals(1.0, count);

        long timerCount = meterRegistry.get(
                        EmailOutboxObservability.DISPATCH_DURATION_TIMER
                )
                .tags(
                        "event_type",
                        "email_verification",
                        "outcome",
                        EmailOutboxObservability.OUTCOME_SENT
                )
                .timer()
                .count();

        assertEquals(1, timerCount);

        double totalDuration = meterRegistry.get(
                        EmailOutboxObservability.DISPATCH_DURATION_TIMER
                )
                .tags(
                        "event_type",
                        "email_verification",
                        "outcome",
                        EmailOutboxObservability.OUTCOME_SENT
                )
                .timer()
                .totalTime(TimeUnit.MILLISECONDS);

        assertEquals(250.0, totalDuration, 1.0);
    }

    @Test
    void recordsPasswordResetDispatchSeparately() {
        observability.recordDispatch(
                EmailNotificationType.PASSWORD_RESET.eventType(),
                EmailOutboxObservability.OUTCOME_SMTP_RETRY,
                Duration.ofMillis(500)
        );

        double count = meterRegistry.get(
                        EmailOutboxObservability.DISPATCH_COUNTER
                )
                .tags(
                        "event_type",
                        "password_reset",
                        "outcome",
                        EmailOutboxObservability.OUTCOME_SMTP_RETRY
                )
                .counter()
                .count();

        assertEquals(1.0, count);
    }

    @Test
    void mapsUnknownEventTypesToBoundedMetricTag() {
        observability.recordDispatch(
                "UNEXPECTED_EVENT_FROM_DATABASE",
                "UNEXPECTED_OUTCOME",
                Duration.ofMillis(10)
        );

        double count = meterRegistry.get(
                        EmailOutboxObservability.DISPATCH_COUNTER
                )
                .tags(
                        "event_type",
                        "other",
                        "outcome",
                        EmailOutboxObservability.OUTCOME_INTERNAL_ERROR
                )
                .counter()
                .count();

        assertEquals(1.0, count);
    }

    @Test
    void preservesLastSuccessfulSnapshotWhenRefreshFails() {
        EmailOutboxOperationalSnapshot snapshot =
                new EmailOutboxOperationalSnapshot(
                        7,
                        2,
                        1,
                        0,
                        10,
                        0,
                        0,
                        0,
                        40.0
                );

        when(outboxStore.loadOperationalSnapshot())
                .thenReturn(snapshot)
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Database unavailable"
                        )
                );

        observability.refreshSnapshot();

        assertGauge(
                EmailOutboxObservability.PENDING_GAUGE,
                7
        );

        observability.refreshSnapshot();

        /*
         * A failed refresh must not replace valid metrics
         * with an artificial zero snapshot.
         */
        assertGauge(
                EmailOutboxObservability.PENDING_GAUGE,
                7
        );

        double failures = meterRegistry.get(
                        EmailOutboxObservability.SNAPSHOT_REFRESH_ERRORS
                )
                .counter()
                .count();

        assertEquals(1.0, failures);
    }

    @Test
    void recordsOutboxPollingFailures() {
        observability.recordPollFailure();

        double failures = meterRegistry.get(
                        EmailOutboxObservability.POLL_ERRORS
                )
                .counter()
                .count();

        assertEquals(1.0, failures);
    }

    private void assertGauge(String metricName, double expected) {
        double actual = meterRegistry.get(metricName)
                .gauge()
                .value();

        assertEquals(expected, actual, 0.001);
    }
}