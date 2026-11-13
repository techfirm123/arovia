
ALTER TABLE user_account
    ADD COLUMN verification_email_last_requested_at TIMESTAMPTZ;


UPDATE user_account AS account
SET verification_email_last_requested_at = COALESCE(
        (
            SELECT MAX(token.created_at)
            FROM email_verification_token AS token
            WHERE token.user_account_id = account.id
        ),
        account.created_at
                                           )
WHERE account.status = 'PENDING_EMAIL_VERIFICATION';


CREATE TABLE identity_rate_limit_bucket
(
    action          VARCHAR(80) NOT NULL,
    subject_hash    CHAR(64) NOT NULL,
    window_start    TIMESTAMPTZ NOT NULL,
    attempt_count   INTEGER NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_identity_rate_limit_bucket
        PRIMARY KEY (action, subject_hash, window_start),

    CONSTRAINT ck_identity_rate_limit_attempt_count
        CHECK (attempt_count > 0),

    CONSTRAINT ck_identity_rate_limit_expiry
        CHECK (expires_at > window_start)
);

CREATE INDEX idx_identity_rate_limit_bucket_expiry
    ON identity_rate_limit_bucket (expires_at);
