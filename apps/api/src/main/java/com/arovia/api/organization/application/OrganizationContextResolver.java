package com.arovia.api.organization.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class OrganizationContextResolver {

    private final OrganizationMembershipService membershipService;

    public OrganizationContextResolver(
            OrganizationMembershipService membershipService
    ) {
        this.membershipService = Objects.requireNonNull(
                membershipService,
                "membershipService must not be null"
        );
    }

    public OrganizationContext resolve(
            UUID authenticatedUserId,
            UUID selectedOrganizationId
    ) {
        Objects.requireNonNull(
                authenticatedUserId,
                "authenticatedUserId must not be null"
        );

        Objects.requireNonNull(
                selectedOrganizationId,
                "selectedOrganizationId must not be null"
        );

        OrganizationMembershipView membership =
                membershipService.getActiveOrganization(
                        authenticatedUserId,
                        selectedOrganizationId
                );

        return new OrganizationContext(
                authenticatedUserId,
                membership.organizationId(),
                membership.organizationName(),
                membership.membershipId(),
                Set.copyOf(membership.roles())
        );
    }
}