package com.arovia.api.customer.domain;

import java.time.Instant;
import java.util.UUID;

public record Customer(
        UUID id,
        UUID organizationId,
        String customerNumber,
        CustomerType customerType,
        String displayName,
        String legalName,
        String email,
        String phone,
        String notes,
        CustomerStatus status,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}