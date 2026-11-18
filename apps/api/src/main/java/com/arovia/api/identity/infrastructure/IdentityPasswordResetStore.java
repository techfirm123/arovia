package com.arovia.api.identity.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.identity.application.InvalidPasswordResetTokenException;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort.AccountResetTarget;
import com.arovia.api.identity.application.port.IdentityPasswordResetPort.PasswordResetTarget;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.NotificationOutbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityPasswordResetStore
        implements IdentityPasswordResetPort {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationOutbox notificationOutbox;

    public IdentityPasswordResetStore(
            JdbcTemplate jdbcTemplate,
            NotificationOutbox notificationOutbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationOutbox = notificationOutbox;
    }

    @Override
    public Optional<AccountResetTarget> findEligibleAccount(
            String normalizedEmail
    ) {
        List<AccountResetTarget> accounts = jdbcTemplate.query(
                """
                SELECT
                    id,
                    email,
                    normalized_email,
                    display_name
                FROM user_account
                WHERE normalized_email = ?
                  AND status = 'ACTIVE'
                  AND email_verified_at IS NOT NULL
                """,
                (resultSet, rowNumber) ->
                        new AccountResetTarget(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("email"),
                                resultSet.getString("normalized_email"),
                                resultSet.getString("display_name")
                        ),
                normalizedEmail
        );

        return accounts.stream().findFirst();
    }

    @Override
    @Transactional
    public boolean createPasswordReset(
            UUID userAccountId,
            UUID resetTokenId,
            String tokenHash,
            Instant expiresAt,
            EmailNotificationRequest notificationRequest,
            String sourceIp,
            String userAgent
    ) {
        List<UUID> eligibleAccounts = jdbcTemplate.query(
                """
                SELECT id
                FROM user_account
                WHERE id = ?
                  AND status = 'ACTIVE'
                  AND email_verified_at IS NOT NULL
                FOR UPDATE
                """,
                (resultSet, rowNumber) ->
                        resultSet.getObject("id", UUID.class),
                userAccountId
        );

        if (eligibleAccounts.isEmpty()) {
            return false;
        }

        /*
         * Revoke every previous active reset token.
         *
         * This also handles previously expired tokens because
         * the partial unique index includes them until revoked.
         */
        jdbcTemplate.update(
                """
                UPDATE password_reset_token
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_account_id = ?
                  AND consumed_at IS NULL
                  AND revoked_at IS NULL
                """,
                userAccountId
        );

        jdbcTemplate.update(
                """
                INSERT INTO password_reset_token (
                    id,
                    user_account_id,
                    token_hash,
                    expires_at
                )
                VALUES (?, ?, ?, ?)
                """,
                resetTokenId,
                userAccountId,
                tokenHash,
                Timestamp.from(expiresAt)
        );

        /*
         * Notification encryption and outbox persistence participate
         * in this transaction. A failure rolls back the reset token.
         */
        notificationOutbox.enqueue(
                userAccountId,
                notificationRequest
        );

        recordAuditEvent(
                userAccountId,
                "USER_PASSWORD_RESET_REQUESTED",
                sourceIp,
                userAgent
        );

        return true;
    }

    @Override
    public Optional<PasswordResetTarget> findValidResetToken(
            String tokenHash
    ) {
        List<PasswordResetTarget> matches = jdbcTemplate.query(
                """
                SELECT
                    account.id,
                    account.normalized_email
                FROM password_reset_token AS reset_token
                INNER JOIN user_account AS account
                    ON account.id = reset_token.user_account_id
                WHERE reset_token.token_hash = ?
                  AND reset_token.consumed_at IS NULL
                  AND reset_token.revoked_at IS NULL
                  AND reset_token.expires_at > CURRENT_TIMESTAMP
                  AND account.status = 'ACTIVE'
                  AND account.email_verified_at IS NOT NULL
                """,
                (resultSet, rowNumber) ->
                        new PasswordResetTarget(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("normalized_email")
                        ),
                tokenHash
        );

        return matches.stream().findFirst();
    }

    @Override
    @Transactional
    public boolean completePasswordReset(
            String tokenHash,
            String newPasswordHash,
            String sourceIp,
            String userAgent
    ) {
        List<LockedResetToken> matches = jdbcTemplate.query(
                """
                SELECT
                    reset_token.id AS reset_token_id,
                    account.id AS user_account_id,
                    account.normalized_email
                FROM password_reset_token AS reset_token
                INNER JOIN user_account AS account
                    ON account.id = reset_token.user_account_id
                WHERE reset_token.token_hash = ?
                  AND reset_token.consumed_at IS NULL
                  AND reset_token.revoked_at IS NULL
                  AND reset_token.expires_at > CURRENT_TIMESTAMP
                  AND account.status = 'ACTIVE'
                  AND account.email_verified_at IS NOT NULL
                FOR UPDATE OF reset_token, account
                """,
                (resultSet, rowNumber) ->
                        new LockedResetToken(
                                resultSet.getObject(
                                        "reset_token_id",
                                        UUID.class
                                ),
                                resultSet.getObject(
                                        "user_account_id",
                                        UUID.class
                                ),
                                resultSet.getString("normalized_email")
                        ),
                tokenHash
        );

        if (matches.isEmpty()) {
            return false;
        }

        LockedResetToken token = matches.getFirst();

        /*
         * Consume the token first. Any subsequent failure throws
         * and rolls back the complete transaction.
         */
        int consumed = jdbcTemplate.update(
                """
                UPDATE password_reset_token
                SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND consumed_at IS NULL
                  AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """,
                token.resetTokenId()
        );

        if (consumed != 1) {
            throw new InvalidPasswordResetTokenException();
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE user_account
                SET
                    password_hash = ?,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                  AND status = 'ACTIVE'
                  AND email_verified_at IS NOT NULL
                """,
                newPasswordHash,
                token.userAccountId()
        );

        if (updated != 1) {
            throw new InvalidPasswordResetTokenException();
        }

        /*
         * Revoke any other outstanding password reset tokens.
         */
        jdbcTemplate.update(
                """
                UPDATE password_reset_token
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_account_id = ?
                  AND id <> ?
                  AND consumed_at IS NULL
                  AND revoked_at IS NULL
                """,
                token.userAccountId(),
                token.resetTokenId()
        );

        /*
         * Revoke every server-side session belonging to this user.
         *
         * Spring Session's principal_name is the normalized email
         * configured as the authentication principal.
         */
        jdbcTemplate.update(
                """
                DELETE FROM spring_session
                WHERE LOWER(principal_name) = ?
                """,
                token.normalizedEmail()
        );

        recordAuditEvent(
                token.userAccountId(),
                "USER_PASSWORD_RESET_COMPLETED",
                sourceIp,
                userAgent
        );

        return true;
    }

    private void recordAuditEvent(
            UUID userAccountId,
            String eventType,
            String sourceIp,
            String userAgent
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO identity_audit_event (
                    id,
                    user_account_id,
                    event_type,
                    source_ip,
                    user_agent
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                userAccountId,
                eventType,
                truncate(sourceIp, 64),
                truncate(userAgent, 512)
        );
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }

        return value.substring(0, maximumLength);
    }

    private record LockedResetToken(
            UUID resetTokenId,
            UUID userAccountId,
            String normalizedEmail
    ) {
    }
}