
package com.arovia.api.identity.infrastructure;

import java.util.List;
import java.util.UUID;

import com.arovia.api.identity.application.InvalidEmailVerificationTokenException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityEmailVerificationStore {

    private static final String PENDING_STATUS =
            "PENDING_EMAIL_VERIFICATION";

    private final JdbcTemplate jdbcTemplate;

    public IdentityEmailVerificationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void activateAccount(
            String tokenHash,
            String sourceIp,
            String userAgent
    ) {
        List<VerificationCandidate> candidates = jdbcTemplate.query(
                """
                SELECT
                    t.id AS token_id,
                    t.user_account_id,
                    u.status,
                    u.email_verified_at
                FROM email_verification_token AS t
                INNER JOIN user_account AS u
                    ON u.id = t.user_account_id
                WHERE t.token_hash = ?
                  AND t.consumed_at IS NULL
                  AND t.revoked_at IS NULL
                  AND t.expires_at > CURRENT_TIMESTAMP
                FOR UPDATE OF t, u
                """,
                (resultSet, rowNumber) -> new VerificationCandidate(
                        resultSet.getObject("token_id", UUID.class),
                        resultSet.getObject("user_account_id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getTimestamp("email_verified_at") != null
                ),
                tokenHash
        );

        if (candidates.isEmpty()) {
            throw new InvalidEmailVerificationTokenException();
        }

        VerificationCandidate candidate = candidates.getFirst();

        if (!PENDING_STATUS.equals(candidate.accountStatus())
                || candidate.emailAlreadyVerified()) {
            throw new InvalidEmailVerificationTokenException();
        }

        int activatedAccounts = jdbcTemplate.update(
                """
                UPDATE user_account
                SET
                    status = 'ACTIVE',
                    email_verified_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                  AND status = 'PENDING_EMAIL_VERIFICATION'
                  AND email_verified_at IS NULL
                """,
                candidate.userAccountId()
        );

        if (activatedAccounts != 1) {
            throw new InvalidEmailVerificationTokenException();
        }

        int consumedTokens = jdbcTemplate.update(
                """
                UPDATE email_verification_token
                SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND consumed_at IS NULL
                  AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """,
                candidate.tokenId()
        );

        if (consumedTokens != 1) {
            throw new InvalidEmailVerificationTokenException();
        }

        jdbcTemplate.update(
                """
                INSERT INTO identity_audit_event (
                    id,
                    user_account_id,
                    event_type,
                    source_ip,
                    user_agent
                )
                VALUES (?, ?, 'USER_EMAIL_VERIFIED', ?, ?)
                """,
                UUID.randomUUID(),
                candidate.userAccountId(),
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

    private record VerificationCandidate(
            UUID tokenId,
            UUID userAccountId,
            String accountStatus,
            boolean emailAlreadyVerified
    ) {
    }
}
