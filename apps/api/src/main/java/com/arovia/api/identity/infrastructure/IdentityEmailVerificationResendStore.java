package com.arovia.api.identity.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.arovia.api.identity.application.port.IdentityEmailVerificationResendPort;
import com.arovia.api.identity.domain.EmailVerificationResendData;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.EmailNotificationType;
import com.arovia.api.notifications.api.NotificationOutbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityEmailVerificationResendStore
        implements IdentityEmailVerificationResendPort {

    private static final String PENDING_STATUS =
            "PENDING_EMAIL_VERIFICATION";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final NotificationOutbox notificationOutbox;

    public IdentityEmailVerificationResendStore(
            JdbcTemplate jdbcTemplate,
            Clock applicationClock,
            NotificationOutbox notificationOutbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = applicationClock;
        this.notificationOutbox = notificationOutbox;
    }

    @Override
    @Transactional
    public boolean resendIfEligible(
            String normalizedEmail,
            EmailVerificationResendData resendData,
            EmailNotificationRequest notificationRequest,
            Duration resendCooldown,
            String sourceIp,
            String userAgent
    ) {
        if (resendCooldown == null
                || resendCooldown.isZero()
                || resendCooldown.isNegative()) {
            throw new IllegalArgumentException(
                    "Email verification resend cooldown must be positive."
            );
        }

        List<AccountState> accounts = jdbcTemplate.query(
                """
                SELECT
                    id,
                    status,
                    email_verified_at,
                    verification_email_last_requested_at
                FROM user_account
                WHERE normalized_email = ?
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> {
                    Timestamp lastRequested =
                            resultSet.getTimestamp(
                                    "verification_email_last_requested_at"
                            );

                    Timestamp verifiedAt =
                            resultSet.getTimestamp("email_verified_at");

                    return new AccountState(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("status"),
                            verifiedAt != null,
                            lastRequested == null
                                    ? null
                                    : lastRequested.toInstant()
                    );
                },
                normalizedEmail
        );

        if (accounts.isEmpty()) {
            return false;
        }

        AccountState account = accounts.getFirst();

        if (!PENDING_STATUS.equals(account.status())
                || account.emailVerified()) {
            return false;
        }

        if (account.lastVerificationEmailRequestedAt() != null) {
            boolean cooldownStillActive = account
                    .lastVerificationEmailRequestedAt()
                    .plus(resendCooldown)
                    .isAfter(clock.instant());

            if (cooldownStillActive) {
                return false;
            }
        }

        /*
         * Supersede previously queued but not yet claimed verification
         * messages. Processing messages cannot safely be cancelled.
         */
        notificationOutbox.supersedePending(
                EmailNotificationType.EMAIL_VERIFICATION,
                account.userAccountId()
        );

        /*
         * Revoke every previous unconsumed token, including expired
         * tokens, before inserting the replacement.
         */
        jdbcTemplate.update(
                """
                UPDATE email_verification_token
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_account_id = ?
                  AND consumed_at IS NULL
                  AND revoked_at IS NULL
                """,
                account.userAccountId()
        );

        jdbcTemplate.update(
                """
                INSERT INTO email_verification_token (
                    id,
                    user_account_id,
                    token_hash,
                    expires_at
                )
                VALUES (?, ?, ?, ?)
                """,
                resendData.verificationTokenId(),
                account.userAccountId(),
                resendData.verificationTokenHash(),
                Timestamp.from(resendData.verificationTokenExpiresAt())
        );

        int updatedAccounts = jdbcTemplate.update(
                """
                UPDATE user_account
                SET
                    verification_email_last_requested_at =
                        CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                  AND status = 'PENDING_EMAIL_VERIFICATION'
                  AND email_verified_at IS NULL
                """,
                account.userAccountId()
        );

        if (updatedAccounts != 1) {
            throw new IllegalStateException(
                    "Unable to update verification request timestamp."
            );
        }

        notificationOutbox.enqueue(
                account.userAccountId(),
                notificationRequest
        );

        jdbcTemplate.update(
                """
                INSERT INTO identity_audit_event (
                    id,
                    user_account_id,
                    event_type,
                    source_ip,
                    user_agent
                )
                VALUES (?, ?, 'USER_EMAIL_VERIFICATION_RESEND_REQUESTED', ?, ?)
                """,
                UUID.randomUUID(),
                account.userAccountId(),
                sourceIp,
                truncate(userAgent, 512)
        );

        return true;
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }

        return value.substring(0, maximumLength);
    }

    private record AccountState(
            UUID userAccountId,
            String status,
            boolean emailVerified,
            java.time.Instant lastVerificationEmailRequestedAt
    ) {
    }
}