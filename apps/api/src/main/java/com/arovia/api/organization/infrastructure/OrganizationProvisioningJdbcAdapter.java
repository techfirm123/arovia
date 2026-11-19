
package com.arovia.api.organization.infrastructure;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.arovia.api.organization.application.CreateOrganizationCommand;
import com.arovia.api.organization.application.OrganizationProvisioningResult;
import com.arovia.api.organization.application.port.OrganizationProvisioningPort;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrganizationProvisioningJdbcAdapter
        implements OrganizationProvisioningPort {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public OrganizationProvisioningJdbcAdapter(
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrganizationProvisioningResult provision(
            UUID ownerUserAccountId,
            CreateOrganizationCommand command
    ) {
        Objects.requireNonNull(ownerUserAccountId);
        Objects.requireNonNull(command);

        Instant now = clock.instant();
        UUID organizationId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        insertOrganization(
                organizationId,
                ownerUserAccountId,
                command.name(),
                now
        );

        insertProfile(organizationId, command.profile(), now);

        if (command.initialLocation() != null) {
            insertInitialLocation(
                    organizationId,
                    command.initialLocation(),
                    now
            );
        }

        insertOwnerMembership(
                membershipId,
                organizationId,
                ownerUserAccountId,
                now
        );

        insertOwnerRole(membershipId, now);

        insertOrganizationAuditEvent(
                organizationId,
                ownerUserAccountId,
                now
        );

        return new OrganizationProvisioningResult(
                organizationId,
                command.name(),
                "ACTIVE",
                membershipId,
                "ACTIVE",
                "OWNER",
                now
        );
    }

    private void insertOrganization(
            UUID organizationId,
            UUID ownerUserAccountId,
            String name,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO organization (
                    id,
                    name,
                    status,
                    created_by_user_id,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, 'ACTIVE', ?, ?, ?)
                """,
                organizationId,
                name,
                ownerUserAccountId,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertProfile(
            UUID organizationId,
            CreateOrganizationCommand.OrganizationProfileInput profile,
            Instant now
    ) {
        if (profile == null) {
            jdbcTemplate.update(
                    """
                    INSERT INTO organization_profile (
                        organization_id,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?)
                    """,
                    organizationId,
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO organization_profile (
                    organization_id,
                    display_name,
                    legal_name,
                    country_code,
                    default_currency,
                    timezone,
                    website_url,
                    contact_email,
                    contact_phone,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                organizationId,
                profile.displayName(),
                profile.legalName(),
                normalizeCode(profile.countryCode()),
                normalizeCode(profile.defaultCurrency()),
                profile.timezone(),
                profile.websiteUrl(),
                profile.contactEmail(),
                profile.contactPhone(),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertInitialLocation(
            UUID organizationId,
            CreateOrganizationCommand.OrganizationLocationInput location,
            Instant now
    ) {
        UUID locationId = UUID.randomUUID();

        String locationType = location.locationType() == null
                || location.locationType().isBlank()
                ? "BUSINESS"
                : location.locationType().trim().toUpperCase();

        jdbcTemplate.update(
                """
                INSERT INTO organization_location (
                    id,
                    organization_id,
                    name,
                    location_code,
                    location_type,
                    address_line_1,
                    address_line_2,
                    city,
                    state_or_region,
                    postal_code,
                    country_code,
                    timezone,
                    is_primary,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE,
                        'ACTIVE', ?, ?)
                """,
                locationId,
                organizationId,
                location.name().trim(),
                location.locationCode(),
                locationType,
                location.addressLine1(),
                location.addressLine2(),
                location.city(),
                location.stateOrRegion(),
                location.postalCode(),
                normalizeCode(location.countryCode()),
                location.timezone(),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertOwnerMembership(
            UUID membershipId,
            UUID organizationId,
            UUID ownerUserAccountId,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO organization_membership (
                    id,
                    organization_id,
                    user_account_id,
                    status,
                    joined_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?)
                """,
                membershipId,
                organizationId,
                ownerUserAccountId,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertOwnerRole(
            UUID membershipId,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO organization_membership_role (
                    membership_id,
                    role_code,
                    assigned_at
                )
                VALUES (?, 'OWNER', ?)
                """,
                membershipId,
                Timestamp.from(now)
        );
    }

    private void insertOrganizationAuditEvent(
            UUID organizationId,
            UUID actorUserAccountId,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO organization_audit_event (
                    id,
                    organization_id,
                    actor_user_id,
                    event_type,
                    occurred_at,
                    details
                )
                VALUES (?, ?, ?, 'ORGANIZATION_CREATED', ?, '{}'::jsonb)
                """,
                UUID.randomUUID(),
                organizationId,
                actorUserAccountId,
                Timestamp.from(now)
        );
    }

    private String normalizeCode(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toUpperCase();
    }
}
