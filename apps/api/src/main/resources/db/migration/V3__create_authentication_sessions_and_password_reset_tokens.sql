/*
 * Spring Session JDBC schema.
 *
 * PRINCIPAL_NAME is VARCHAR(254), rather than the default 100,
 * because Arovia uses normalized email addresses as principal names.
 */

CREATE TABLE spring_session
(
    primary_id              CHAR(36) NOT NULL,
    session_id              CHAR(36) NOT NULL,
    creation_time           BIGINT NOT NULL,
    last_access_time        BIGINT NOT NULL,
    max_inactive_interval   INT NOT NULL,
    expiry_time             BIGINT NOT NULL,
    principal_name          VARCHAR(254),

    CONSTRAINT spring_session_pk
        PRIMARY KEY (primary_id),

    CONSTRAINT ck_spring_session_max_inactive_interval
        CHECK (max_inactive_interval > 0)
);

CREATE UNIQUE INDEX spring_session_ix1
    ON spring_session (session_id);

CREATE INDEX spring_session_ix2
    ON spring_session (expiry_time);

CREATE INDEX spring_session_ix3
    ON spring_session (principal_name);


CREATE TABLE spring_session_attributes
(
    session_primary_id  CHAR(36) NOT NULL,
    attribute_name      VARCHAR(200) NOT NULL,
    attribute_bytes     BYTEA NOT NULL,

    CONSTRAINT spring_session_attributes_pk
        PRIMARY KEY (session_primary_id, attribute_name),

    CONSTRAINT spring_session_attributes_fk
        FOREIGN KEY (session_primary_id)
            REFERENCES spring_session (primary_id)
            ON DELETE CASCADE
);


/*
 * Password recovery foundation.
 *
 * Only the SHA-256 token hash is persisted.
 * The raw reset token will be delivered through the encrypted outbox.
 */

CREATE TABLE password_reset_token
(
    id                  UUID PRIMARY KEY,
    user_account_id     UUID NOT NULL,
    token_hash          CHAR(64) NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    consumed_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_token_user
        FOREIGN KEY (user_account_id)
            REFERENCES user_account (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_password_reset_token_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_password_reset_token_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT ck_password_reset_token_terminal_state
        CHECK (
            consumed_at IS NULL
                OR revoked_at IS NULL
            )
);

CREATE INDEX idx_password_reset_token_user_created
    ON password_reset_token (
                             user_account_id,
                             created_at DESC
        );

CREATE INDEX idx_password_reset_token_expiry
    ON password_reset_token (expires_at);

CREATE UNIQUE INDEX uq_password_reset_token_active_user
    ON password_reset_token (user_account_id)
    WHERE consumed_at IS NULL
      AND revoked_at IS NULL;