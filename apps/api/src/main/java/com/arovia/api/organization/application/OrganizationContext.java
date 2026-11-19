package com.arovia.api.organization.application;

import java.util.Set;
import java.util.UUID;

public record OrganizationContext(
        UUID userAccountId,
        UUID organizationId,
        String organizationName,
        UUID membershipId,
        Set<String> roles
) {
    public OrganizationContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... expectedRoles) {
        if (expectedRoles == null) {
            return false;
        }

        for (String role : expectedRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }

        return false;
    }
}