
package com.arovia.api.identity.infrastructure;

import java.sql.Timestamp;
import java.util.UUID;

import com.arovia.api.identity.domain.RegistrationData;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityRegistrationStore {

    private final JdbcTemplate jdbcTemplate;

    public IdentityRegistrationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createPendingAccount(
            RegistrationData registration,
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
                    status
                )
                VALUES (?, ?, ?, ?, ?, 'PENDING_EMAIL_VERIFICATION')
                ON CONFLICT (normalized_email) DO NOTHING
                """,
                registration.userAccountId(),
                registration.email(),
                registration.normalizedEmail(),
                registration.passwordHash(),
                registration.displayName()
        );

        /*
         * Duplicate normalized email:
         * return without creating a token, outbox record or audit event.
         *
         * The API still returns the same generic response.
         */
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
                    attempt_count
                )
                VALUES (?, ?, 'USER_ACCOUNT', ?, ?, ?, ?, 'PENDING', 0)
                """,
                registration.outboxId(),
                registration.outboxEventType(),
                registration.userAccountId(),
                registration.encryptedOutboxPayload().ciphertext(),
                registration.encryptedOutboxPayload().nonce(),
                registration.encryptedOutboxPayload().keyVersion()
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
