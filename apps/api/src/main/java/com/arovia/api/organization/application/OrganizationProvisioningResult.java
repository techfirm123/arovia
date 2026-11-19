
package com.arovia.api.organization.application;

import java.time.Instant;
import java.util.UUID;

public record OrganizationProvisioningResult(
        UUID organizationId,
        String organizationName,
        String organizationStatus,
        UUID membershipId,
        String membershipStatus,
        String assignedRole,
        Instant createdAt
) {
}
