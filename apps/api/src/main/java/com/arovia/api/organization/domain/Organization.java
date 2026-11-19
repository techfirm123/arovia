
package com.arovia.api.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record Organization(
        UUID id,
        String name,
        OrganizationStatus status,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
