
package com.arovia.api.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record OrganizationMembership(
        UUID id,
        UUID organizationId,
        UUID userAccountId,
        OrganizationMembershipStatus status,
        Instant joinedAt,
        Instant createdAt
) {
}
