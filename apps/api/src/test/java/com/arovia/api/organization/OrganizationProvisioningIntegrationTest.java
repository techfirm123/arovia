package com.arovia.api.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import com.arovia.api.organization.application.CreateOrganizationCommand;
import com.arovia.api.organization.application.OrganizationProvisioningResult;
import com.arovia.api.organization.application.OrganizationProvisioningService;
import com.arovia.api.testing.PostgresIntegrationTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "arovia.scheduling.enabled=false"
        }
)
@Tag("integration")
class OrganizationProvisioningIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrganizationProvisioningService provisioningService;

    @DynamicPropertySource
    static void registerIntegrationTestProperties(
            DynamicPropertyRegistry registry
    ) {
        PostgresIntegrationTestSupport.registerProperties(
                registry,
                "integration-test-v1"
        );
    }

    @BeforeEach
    void cleanOrganizationData() {
        /*
         * This test suite may only clear organization data
         * from the dedicated arovia_test database.
         */
        jdbcTemplate.execute(
                "TRUNCATE TABLE organization CASCADE"
        );

        jdbcTemplate.execute(
                "TRUNCATE TABLE user_account CASCADE"
        );
    }

    @Test
    void provisionsOrganizationWithProfileLocationAndOwnerMembership() {
        UUID ownerUserId = createVerifiedUser(
                "organization-owner@example.com"
        );

        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "Arovia Computer Store",
                        new CreateOrganizationCommand.OrganizationProfileInput(
                                "Arovia Computer Store",
                                "Arovia Computer Store Private Limited",
                                "IN",
                                "INR",
                                "Asia/Kolkata",
                                "https://example.com",
                                "owner@example.com",
                                "+919876543210"
                        ),
                        new CreateOrganizationCommand.OrganizationLocationInput(
                                "Main Store",
                                "MAIN",
                                "BUSINESS",
                                "123 Main Road",
                                null,
                                "Bengaluru",
                                "Karnataka",
                                "560001",
                                "IN",
                                "Asia/Kolkata"
                        )
                );

        OrganizationProvisioningResult result =
                provisioningService.createOrganization(
                        ownerUserId,
                        true,
                        command
                );

        assertNotNull(result);
        assertNotNull(result.organizationId());
        assertNotNull(result.membershipId());

        assertEquals(
                1,
                count(
                        "SELECT COUNT(*) FROM organization WHERE id = ?",
                        result.organizationId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_profile
                        WHERE organization_id = ?
                          AND display_name = ?
                          AND country_code = ?
                          AND default_currency = ?
                        """,
                        result.organizationId(),
                        "Arovia Computer Store",
                        "IN",
                        "INR"
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_location
                        WHERE organization_id = ?
                          AND location_code = ?
                          AND is_primary = TRUE
                          AND status = 'ACTIVE'
                        """,
                        result.organizationId(),
                        "MAIN"
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_membership
                        WHERE id = ?
                          AND organization_id = ?
                          AND user_account_id = ?
                          AND status = 'ACTIVE'
                        """,
                        result.membershipId(),
                        result.organizationId(),
                        ownerUserId
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_membership_role
                        WHERE membership_id = ?
                          AND role_code = 'OWNER'
                        """,
                        result.membershipId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_audit_event
                        WHERE organization_id = ?
                          AND actor_user_id = ?
                          AND event_type = 'ORGANIZATION_CREATED'
                        """,
                        result.organizationId(),
                        ownerUserId
                )
        );
    }

    @Test
    void provisionsOrganizationWithoutInitialLocation() {
        UUID ownerUserId = createVerifiedUser(
                "organization-no-location@example.com"
        );

        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "Arovia New Business",
                        null,
                        null
                );

        OrganizationProvisioningResult result =
                provisioningService.createOrganization(
                        ownerUserId,
                        true,
                        command
                );

        assertNotNull(result.organizationId());

        assertEquals(
                1,
                count(
                        "SELECT COUNT(*) FROM organization WHERE id = ?",
                        result.organizationId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_profile
                        WHERE organization_id = ?
                        """,
                        result.organizationId()
                )
        );

        assertEquals(
                0,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_location
                        WHERE organization_id = ?
                        """,
                        result.organizationId()
                )
        );

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_membership_role
                        WHERE membership_id = ?
                          AND role_code = 'OWNER'
                        """,
                        result.membershipId()
                )
        );
    }

    @Test
    void allowsOneUserToOwnMultipleOrganizations() {
        UUID ownerUserId = createVerifiedUser(
                "multi-organization-owner@example.com"
        );

        OrganizationProvisioningResult first =
                provisioningService.createOrganization(
                        ownerUserId,
                        true,
                        new CreateOrganizationCommand(
                                "First Business",
                                null,
                                null
                        )
                );

        OrganizationProvisioningResult second =
                provisioningService.createOrganization(
                        ownerUserId,
                        true,
                        new CreateOrganizationCommand(
                                "Second Business",
                                null,
                                null
                        )
                );

        assertNotNull(first.organizationId());
        assertNotNull(second.organizationId());

        assertEquals(
                2,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_membership
                        WHERE user_account_id = ?
                          AND status = 'ACTIVE'
                        """,
                        ownerUserId
                )
        );

        assertEquals(
                2,
                count(
                        """
                        SELECT COUNT(*)
                        FROM organization_membership_role AS role
                        INNER JOIN organization_membership AS membership
                            ON membership.id = role.membership_id
                        WHERE membership.user_account_id = ?
                          AND role.role_code = 'OWNER'
                        """,
                        ownerUserId
                )
        );
    }

    private UUID createVerifiedUser(String email) {
        UUID userId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO user_account (
                    id,
                    email,
                    normalized_email,
                    password_hash,
                    display_name,
                    status,
                    email_verified_at
                )
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                """,
                userId,
                email,
                email.toLowerCase(),
                "integration-test-password-hash",
                "Organization Integration Test"
        );

        return userId;
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                arguments
        );

        return result == null ? 0 : result;
    }
}