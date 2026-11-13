CREATE TABLE user_account
(
    id                  UUID PRIMARY KEY,
    email               VARCHAR(254) NOT NULL,
    normalized_email    VARCHAR(254) NOT NULL,
    password_hash       VARCHAR(512) NOT NULL,
    display_name        VARCHAR(120) NOT NULL,
    status              VARCHAR(40) NOT NULL,
    email_verified_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_user_account_normalized_email
        UNIQUE (normalized_email),

    CONSTRAINT ck_user_account_status
        CHECK (
            status IN (
                'PENDING_EMAIL_VERIFICATION',
                'ACTIVE',
                'SUSPENDED',
                'DEACTIVATED'
            )
        ),

    CONSTRAINT ck_user_account_version
        CHECK (version >= 0)
);

CREATE INDEX idx_user_account_status
    ON user_account (status);


CREATE TABLE email_verification_token
(
    id                  UUID PRIMARY KEY,
    user_account_id     UUID NOT NULL,
    token_hash          CHAR(64) NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    consumed_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_verification_token_user
        FOREIGN KEY (user_account_id)
        REFERENCES user_account (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_email_verification_token_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_email_verification_token_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT ck_email_verification_token_terminal_state
        CHECK (
            consumed_at IS NULL
            OR revoked_at IS NULL
        )
);

CREATE INDEX idx_email_verification_token_user_created
    ON email_verification_token (
        user_account_id,
        created_at DESC
    );

CREATE INDEX idx_email_verification_token_expiry
    ON email_verification_token (expires_at);

CREATE UNIQUE INDEX uq_email_verification_token_active_user
    ON email_verification_token (user_account_id)
    WHERE consumed_at IS NULL
      AND revoked_at IS NULL;


CREATE TABLE email_outbox
(
    id                      UUID PRIMARY KEY,
    event_type              VARCHAR(80) NOT NULL,
    aggregate_type          VARCHAR(80) NOT NULL,
    aggregate_id            UUID NOT NULL,

    payload_ciphertext      BYTEA,
    payload_nonce           BYTEA,
    encryption_key_version  VARCHAR(80) NOT NULL,

    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count           INTEGER NOT NULL DEFAULT 0,
    next_attempt_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_until            TIMESTAMPTZ,
    last_error_code         VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at                 TIMESTAMPTZ,

    CONSTRAINT ck_email_outbox_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'RETRY',
                'SENT',
                'DEAD_LETTER'
            )
        ),

    CONSTRAINT ck_email_outbox_attempt_count
        CHECK (attempt_count >= 0),

    CONSTRAINT ck_email_outbox_payload_pair
        CHECK (
            (payload_ciphertext IS NULL AND payload_nonce IS NULL)
            OR
            (payload_ciphertext IS NOT NULL AND payload_nonce IS NOT NULL)
        ),

    CONSTRAINT ck_email_outbox_sent_timestamp
        CHECK (
            (status = 'SENT' AND sent_at IS NOT NULL)
            OR
            (status <> 'SENT' AND sent_at IS NULL)
        ),

    CONSTRAINT ck_email_outbox_sent_payload
        CHECK (
            status <> 'SENT'
            OR payload_ciphertext IS NULL
        )
);

CREATE INDEX idx_email_outbox_claimable
    ON email_outbox (
        status,
        next_attempt_at,
        created_at
    );

CREATE INDEX idx_email_outbox_aggregate
    ON email_outbox (
        aggregate_type,
        aggregate_id
    );


CREATE TABLE identity_audit_event
(
    id                  UUID PRIMARY KEY,
    user_account_id     UUID,
    event_type          VARCHAR(100) NOT NULL,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_ip           VARCHAR(64),
    user_agent          VARCHAR(512),
    correlation_id      VARCHAR(100),
    details             JSONB NOT NULL DEFAULT '{}'::JSONB,

    CONSTRAINT fk_identity_audit_event_user
        FOREIGN KEY (user_account_id)
        REFERENCES user_account (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_identity_audit_event_user_occurred
    ON identity_audit_event (
        user_account_id,
        occurred_at DESC
    );

CREATE INDEX idx_identity_audit_event_type_occurred
    ON identity_audit_event (
        event_type,
        occurred_at DESC
    );