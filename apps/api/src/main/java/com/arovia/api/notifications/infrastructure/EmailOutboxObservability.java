package com.arovia.api.notifications.infrastructure;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.domain.EmailOutboxOperationalSnapshot;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataAccessException;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailOutboxObservability {

    private static final Logger log =
            LoggerFactory.getLogger(EmailOutboxObservability.class);

    public static final String PENDING_GAUGE =
            "arovia.notifications.outbox.pending";

    public static final String RETRY_GAUGE =
            "arovia.notifications.outbox.retry";

    public static final String PROCESSING_GAUGE =
            "arovia.notifications.outbox.processing";

    public static final String DEAD_LETTER_GAUGE =
            "arovia.notifications.outbox.dead_letter";

    public static final String RETAINED_PAYLOAD_GAUGE =
            "arovia.notifications.outbox.retained_payload";

    public static final String DEAD_LETTER_RETAINED_PAYLOAD_GAUGE =
            "arovia.notifications.outbox.dead_letter.retained_payload";

    public static final String ENCRYPTION_KEY_UNAVAILABLE_GAUGE =
            "arovia.notifications.outbox.encryption_key_unavailable";

    public static final String EXPIRED_PROCESSING_GAUGE =
            "arovia.notifications.outbox.expired_processing";

    public static final String OLDEST_PENDING_AGE_GAUGE =
            "arovia.notifications.outbox.oldest_pending_age_seconds";

    public static final String DISPATCH_COUNTER =
            "arovia.notifications.outbox.dispatches";

    public static final String DISPATCH_DURATION_TIMER =
            "arovia.notifications.outbox.dispatch.duration";

    public static final String SNAPSHOT_REFRESH_ERRORS =
            "arovia.notifications.outbox.snapshot.refresh.errors";

    public static final String POLL_ERRORS =
            "arovia.notifications.outbox.poll.errors";

    public static final String OUTCOME_SENT = "sent";

    public static final String OUTCOME_SMTP_RETRY = "smtp_retry";

    public static final String OUTCOME_SMTP_DEAD_LETTER =
            "smtp_dead_letter";

    public static final String OUTCOME_ENCRYPTION_KEY_UNAVAILABLE =
            "encryption_key_unavailable";

    public static final String OUTCOME_UNSUPPORTED_EVENT_TYPE =
            "unsupported_event_type";

    public static final String OUTCOME_PAYLOAD_PROCESSING_FAILED =
            "payload_processing_failed";

    public static final String OUTCOME_INTERNAL_ERROR = "internal_error";

    private static final Set<String> ALLOWED_OUTCOMES = Set.of(
            OUTCOME_SENT,
            OUTCOME_SMTP_RETRY,
            OUTCOME_SMTP_DEAD_LETTER,
            OUTCOME_ENCRYPTION_KEY_UNAVAILABLE,
            OUTCOME_UNSUPPORTED_EVENT_TYPE,
            OUTCOME_PAYLOAD_PROCESSING_FAILED,
            OUTCOME_INTERNAL_ERROR
    );

    private final MeterRegistry meterRegistry;

    private final EmailOutboxStore outboxStore;

    private final AtomicReference<EmailOutboxOperationalSnapshot>
            latestSnapshot = new AtomicReference<>(
            EmailOutboxOperationalSnapshot.empty()
    );

    public EmailOutboxObservability(
            MeterRegistry meterRegistry,
            EmailOutboxStore outboxStore
    ) {
        this.meterRegistry = meterRegistry;
        this.outboxStore = outboxStore;

        registerGauges();

        meterRegistry.counter(SNAPSHOT_REFRESH_ERRORS);
        meterRegistry.counter(POLL_ERRORS);
    }

    /**
     * Obtain the first database snapshot after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshAfterApplicationReady() {
        refreshSnapshot();
    }

    /**
     * Refresh all database-backed gauges using one aggregate query.
     *
     * A failed refresh retains the last known-good snapshot.
     */
    @Scheduled(
            fixedDelayString =
                    "${arovia.outbox.observability.refresh-interval-ms:30000}"
    )
    public void refreshSnapshot() {
        try {
            EmailOutboxOperationalSnapshot snapshot =
                    outboxStore.loadOperationalSnapshot();

            latestSnapshot.set(snapshot);

        } catch (DataAccessException exception) {
            meterRegistry.counter(SNAPSHOT_REFRESH_ERRORS).increment();

            log.error(
                    "Unable to refresh email outbox operational metrics. "
                            + "The previous successful snapshot is retained. "
                            + "errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    /**
     * Records one completed or failed dispatch operation.
     *
     * Event types and outcomes are normalized to bounded values to
     * prevent arbitrary database content from creating high-cardinality
     * metrics.
     */
    public void recordDispatch(
            String eventType,
            String outcome,
            Duration duration
    ) {
        String safeEventType = normalizeEventType(eventType);
        String safeOutcome = normalizeOutcome(outcome);

        meterRegistry.counter(
                DISPATCH_COUNTER,
                "event_type",
                safeEventType,
                "outcome",
                safeOutcome
        ).increment();

        Duration safeDuration = duration == null
                ? Duration.ZERO
                : duration.isNegative()
                ? Duration.ZERO
                : duration;

        meterRegistry.timer(
                DISPATCH_DURATION_TIMER,
                "event_type",
                safeEventType,
                "outcome",
                safeOutcome
        ).record(safeDuration);
    }

    /**
     * Records a failure while claiming an outbox message.
     */
    public void recordPollFailure() {
        meterRegistry.counter(POLL_ERRORS).increment();
    }

    private void registerGauges() {
        registerGauge(
                PENDING_GAUGE,
                "Email outbox messages waiting for first delivery.",
                EmailOutboxOperationalSnapshot::pendingCount
        );

        registerGauge(
                RETRY_GAUGE,
                "Email outbox messages waiting for retry.",
                EmailOutboxOperationalSnapshot::retryCount
        );

        registerGauge(
                PROCESSING_GAUGE,
                "Email outbox messages currently being processed.",
                EmailOutboxOperationalSnapshot::processingCount
        );

        registerGauge(
                DEAD_LETTER_GAUGE,
                "Email outbox messages in the dead-letter state.",
                EmailOutboxOperationalSnapshot::deadLetterCount
        );

        registerGauge(
                RETAINED_PAYLOAD_GAUGE,
                "Email outbox records retaining encrypted payloads.",
                EmailOutboxOperationalSnapshot::retainedPayloadCount
        );

        registerGauge(
                DEAD_LETTER_RETAINED_PAYLOAD_GAUGE,
                "Dead-letter records retaining encrypted payloads.",
                EmailOutboxOperationalSnapshot::deadLetterRetainedPayloadCount
        );

        registerGauge(
                ENCRYPTION_KEY_UNAVAILABLE_GAUGE,
                "Retained payloads waiting for an unavailable encryption key.",
                EmailOutboxOperationalSnapshot::encryptionKeyUnavailableCount
        );

        registerGauge(
                EXPIRED_PROCESSING_GAUGE,
                "Processing records with an expired or missing lock.",
                EmailOutboxOperationalSnapshot::expiredProcessingCount
        );

        registerGauge(
                OLDEST_PENDING_AGE_GAUGE,
                "Age in seconds of the oldest active outbox message.",
                EmailOutboxOperationalSnapshot::oldestPendingAgeSeconds
        );
    }

    private void registerGauge(
            String name,
            String description,
            java.util.function.ToDoubleFunction<
                    EmailOutboxOperationalSnapshot
                    > valueFunction
    ) {
        Gauge.builder(
                        name,
                        latestSnapshot,
                        reference -> valueFunction.applyAsDouble(reference.get())
                )
                .description(description)
                .register(meterRegistry);
    }

    private String normalizeEventType(String eventType) {
        if (EmailNotificationType.EMAIL_VERIFICATION.eventType()
                .equals(eventType)) {
            return "email_verification";
        }

        if (EmailNotificationType.PASSWORD_RESET.eventType()
                .equals(eventType)) {
            return "password_reset";
        }

        return "other";
    }

    private String normalizeOutcome(String outcome) {
        if (outcome != null && ALLOWED_OUTCOMES.contains(outcome)) {
            return outcome;
        }

        return OUTCOME_INTERNAL_ERROR;
    }
}