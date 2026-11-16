package com.arovia.api.identity.infrastructure;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityAuthenticationAuditStore {

    private final JdbcTemplate jdbcTemplate;

    public IdentityAuthenticationAuditStore(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void record(
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

    private String truncate(
            String value,
            int maximumLength
    ) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }

        return value.substring(0, maximumLength);
    }
}