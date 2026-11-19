
-- AROVIA-002B
-- Industry-neutral organization provisioning foundation.

CREATE TABLE organization (
                              id UUID PRIMARY KEY,
                              name VARCHAR(160) NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                              created_by_user_id UUID NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              version BIGINT NOT NULL DEFAULT 0,

                              CONSTRAINT fk_organization_created_by
                                  FOREIGN KEY (created_by_user_id)
                                      REFERENCES user_account (id)
                                      ON DELETE RESTRICT,

                              CONSTRAINT ck_organization_name_not_blank
                                  CHECK (length(trim(name)) > 0),

                              CONSTRAINT ck_organization_status
                                  CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX idx_organization_created_by
    ON organization (created_by_user_id);

CREATE INDEX idx_organization_status
    ON organization (status);


CREATE TABLE organization_profile (
                                      organization_id UUID PRIMARY KEY,
                                      display_name VARCHAR(160),
                                      legal_name VARCHAR(200),
                                      country_code VARCHAR(2),
                                      default_currency VARCHAR(3),
                                      timezone VARCHAR(64),
                                      website_url VARCHAR(500),
                                      contact_email VARCHAR(320),
                                      contact_phone VARCHAR(40),
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_organization_profile_organization
                                          FOREIGN KEY (organization_id)
                                              REFERENCES organization (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT ck_organization_profile_country_code
                                          CHECK (
                                              country_code IS NULL
                                                  OR country_code ~ '^[A-Z]{2}$'
),

    CONSTRAINT ck_organization_profile_currency
        CHECK (
            default_currency IS NULL
            OR default_currency ~ '^[A-Z]{3}$'
        )
);


CREATE TABLE organization_location (
                                       id UUID PRIMARY KEY,
                                       organization_id UUID NOT NULL,
                                       name VARCHAR(120) NOT NULL,
                                       location_code VARCHAR(40),
                                       location_type VARCHAR(30) NOT NULL DEFAULT 'BUSINESS',
                                       address_line_1 VARCHAR(200),
                                       address_line_2 VARCHAR(200),
                                       city VARCHAR(120),
                                       state_or_region VARCHAR(120),
                                       postal_code VARCHAR(24),
                                       country_code VARCHAR(2),
                                       timezone VARCHAR(64),
                                       is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_organization_location_organization
                                           FOREIGN KEY (organization_id)
                                               REFERENCES organization (id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT ck_organization_location_name_not_blank
                                           CHECK (length(trim(name)) > 0),

                                       CONSTRAINT ck_organization_location_type
                                           CHECK (
                                               location_type IN (
                                                                 'BUSINESS',
                                                                 'WAREHOUSE',
                                                                 'SERVICE_CENTER',
                                                                 'OFFICE'
                                                   )
                                               ),

                                       CONSTRAINT ck_organization_location_status
                                           CHECK (status IN ('ACTIVE', 'INACTIVE')),

                                       CONSTRAINT ck_organization_location_country_code
                                           CHECK (
                                               country_code IS NULL
                                                   OR country_code ~ '^[A-Z]{2}$'
)
    );

CREATE UNIQUE INDEX uq_organization_location_code
    ON organization_location (organization_id, location_code)
    WHERE location_code IS NOT NULL;

CREATE UNIQUE INDEX uq_organization_primary_location
    ON organization_location (organization_id)
    WHERE is_primary = TRUE AND status = 'ACTIVE';

CREATE INDEX idx_organization_location_organization
    ON organization_location (organization_id);


CREATE TABLE organization_membership (
                                         id UUID PRIMARY KEY,
                                         organization_id UUID NOT NULL,
                                         user_account_id UUID NOT NULL,
                                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                         joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         ended_at TIMESTAMPTZ,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT fk_organization_membership_organization
                                             FOREIGN KEY (organization_id)
                                                 REFERENCES organization (id)
                                                 ON DELETE RESTRICT,

                                         CONSTRAINT fk_organization_membership_user
                                             FOREIGN KEY (user_account_id)
                                                 REFERENCES user_account (id)
                                                 ON DELETE RESTRICT,

                                         CONSTRAINT uq_organization_membership_org_user
                                             UNIQUE (organization_id, user_account_id),

                                         CONSTRAINT ck_organization_membership_status
                                             CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED')),

                                         CONSTRAINT ck_organization_membership_lifecycle
                                             CHECK (
                                                 (status = 'ACTIVE' AND ended_at IS NULL)
                                                     OR
                                                 (status IN ('SUSPENDED', 'REMOVED') AND ended_at IS NOT NULL)
                                                 )
);

CREATE INDEX idx_organization_membership_user
    ON organization_membership (user_account_id);

CREATE INDEX idx_organization_membership_organization_status
    ON organization_membership (organization_id, status);


CREATE TABLE organization_membership_role (
                                              membership_id UUID NOT NULL,
                                              role_code VARCHAR(40) NOT NULL,
                                              assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                              CONSTRAINT pk_organization_membership_role
                                                  PRIMARY KEY (membership_id, role_code),

                                              CONSTRAINT fk_organization_membership_role_membership
                                                  FOREIGN KEY (membership_id)
                                                      REFERENCES organization_membership (id)
                                                      ON DELETE CASCADE,

                                              CONSTRAINT ck_organization_membership_role_code
                                                  CHECK (
                                                      role_code IN (
                                                                    'OWNER',
                                                                    'ADMIN',
                                                                    'MEMBER',
                                                                    'SALES',
                                                                    'TECHNICIAN',
                                                                    'INVENTORY',
                                                                    'FINANCE',
                                                                    'VIEWER'
                                                          )
                                                      )
);


CREATE TABLE organization_audit_event (
                                          id UUID PRIMARY KEY,
                                          organization_id UUID NOT NULL,
                                          actor_user_id UUID,
                                          event_type VARCHAR(100) NOT NULL,
                                          occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          correlation_id VARCHAR(100),
                                          details JSONB NOT NULL DEFAULT '{}'::JSONB,

                                          CONSTRAINT fk_organization_audit_event_organization
                                              FOREIGN KEY (organization_id)
                                                  REFERENCES organization (id)
                                                  ON DELETE RESTRICT,

                                          CONSTRAINT fk_organization_audit_event_actor
                                              FOREIGN KEY (actor_user_id)
                                                  REFERENCES user_account (id)
                                                  ON DELETE SET NULL,

                                          CONSTRAINT ck_organization_audit_event_type_not_blank
                                              CHECK (length(trim(event_type)) > 0)
);

CREATE INDEX idx_organization_audit_event_org_time
    ON organization_audit_event (organization_id, occurred_at DESC);

CREATE INDEX idx_organization_audit_event_actor
    ON organization_audit_event (actor_user_id, occurred_at DESC);
