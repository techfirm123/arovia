-- AROVIA-003: reusable, organization-scoped customer capability.

CREATE TABLE customer_number_sequence (
                                          organization_id UUID PRIMARY KEY,
                                          next_number BIGINT NOT NULL DEFAULT 1,

                                          CONSTRAINT fk_customer_number_sequence_organization
                                              FOREIGN KEY (organization_id)
                                                  REFERENCES organization (id)
                                                  ON DELETE RESTRICT,

                                          CONSTRAINT ck_customer_number_sequence_next_number
                                              CHECK (next_number >= 1)
);

CREATE TABLE customer (
                          id UUID PRIMARY KEY,
                          organization_id UUID NOT NULL,
                          customer_number VARCHAR(32) NOT NULL,
                          customer_type VARCHAR(20) NOT NULL,
                          display_name VARCHAR(160) NOT NULL,
                          legal_name VARCHAR(200),
                          email VARCHAR(320),
                          phone VARCHAR(40),
                          notes VARCHAR(2000),
                          status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                          created_by_user_id UUID NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          version BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT fk_customer_organization
                              FOREIGN KEY (organization_id)
                                  REFERENCES organization (id)
                                  ON DELETE RESTRICT,

                          CONSTRAINT fk_customer_created_by
                              FOREIGN KEY (created_by_user_id)
                                  REFERENCES user_account (id)
                                  ON DELETE RESTRICT,

                          CONSTRAINT uq_customer_organization_number
                              UNIQUE (organization_id, customer_number),

                          CONSTRAINT ck_customer_type
                              CHECK (customer_type IN ('INDIVIDUAL', 'BUSINESS')),

                          CONSTRAINT ck_customer_status
                              CHECK (status IN ('ACTIVE', 'ARCHIVED')),

                          CONSTRAINT ck_customer_display_name_not_blank
                              CHECK (length(trim(display_name)) > 0),

                          CONSTRAINT ck_customer_email_not_blank
                              CHECK (email IS NULL OR length(trim(email)) > 0),

                          CONSTRAINT ck_customer_phone_not_blank
                              CHECK (phone IS NULL OR length(trim(phone)) > 0)
);

CREATE INDEX idx_customer_org_status_created
    ON customer (organization_id, status, created_at DESC, id DESC);

CREATE INDEX idx_customer_org_number
    ON customer (organization_id, customer_number);

CREATE INDEX idx_customer_org_display_name
    ON customer (organization_id, lower(display_name));

CREATE INDEX idx_customer_org_email
    ON customer (organization_id, lower(email))
    WHERE email IS NOT NULL;

CREATE INDEX idx_customer_org_phone
    ON customer (organization_id, phone)
    WHERE phone IS NOT NULL;