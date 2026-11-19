
package com.arovia.api.organization.web;

import java.time.Instant;
import java.util.UUID;

import com.arovia.api.organization.application.OrganizationProvisioningResult;

public record OrganizationResponse(
        UUID id,
        String name,
        String status,
        UUID membershipId,
        String membershipStatus,
        String role,
        Instant createdAt
) {

    public static OrganizationResponse from(
            OrganizationProvisioningResult result
    ) {
        return new OrganizationResponse(
                result.organizationId(),
                result.organizationName(),
                result.organizationStatus(),
                result.membershipId(),
                result.membershipStatus(),
                result.assignedRole(),
                result.createdAt()
        );
    }
}
