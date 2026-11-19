package com.arovia.api.organization.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrganizationMembershipView(
        UUID organizationId,
        String organizationName,
        String organizationStatus,
        String displayName,
        UUID membershipId,
        String membershipStatus,
        Instant joinedAt,
        List<String> roles
) {
    public OrganizationMembershipView {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}