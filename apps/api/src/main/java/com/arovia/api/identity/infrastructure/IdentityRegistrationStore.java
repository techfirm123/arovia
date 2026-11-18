package com.arovia.api.identity.infrastructure;

import java.sql.Timestamp;
import java.util.UUID;

import com.arovia.api.identity.application.port.IdentityRegistrationPort;
import com.arovia.api.identity.domain.RegistrationData;
import com.arovia.api.notifications.api.EmailNotificationRequest;
import com.arovia.api.notifications.api.NotificationOutbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityRegistrationStore implements IdentityRegistrationPort {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationOutbox notificationOutbox;

    public IdentityRegistrationStore(
            JdbcTemplate jdbcTemplate,
            NotificationOutbox notificationOutbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationOutbox = notificationOutbox;
    }

    @Override
    @Transactional
    public void createPendingAccount(
            RegistrationData registration,
            EmailNotificationRequest notificationRequest,
            String sourceIp,
            String userAgent
    ) {
        int insertedAccounts = jdbcTemplate.update(
                """
                INSERT INTO user_account (
                    id,
                    email,
                    normalized_email,
                    password_hash,
                    display_name,
                    status,
                    verification_email_last_requested_at
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    'PENDING_EMAIL_VERIFICATION',
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (normalized_email) DO NOTHING
                """,
                registration.userAccountId(),
                registration.email(),
                registration.normalizedEmail(),
                registration.passwordHash(),
                registration.displayName()
        );

        if (insertedAccounts == 0) {
            return;
        }

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
                registration.verificationTokenId(),
                registration.userAccountId(),
                registration.verificationTokenHash(),
                Timestamp.from(registration.verificationTokenExpiresAt())
        );

        /*
         * Notification enqueue joins this transaction. If encryption or
         * outbox persistence fails, account and token inserts roll back.
         */
        notificationOutbox.enqueue(
                registration.userAccountId(),
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
                VALUES (?, ?, 'USER_REGISTRATION_ACCEPTED', ?, ?)
                """,
                UUID.randomUUID(),
                registration.userAccountId(),
                sourceIp,
                truncate(userAgent, 512)
        );
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }

        return value.substring(0, maximumLength);
    }
}