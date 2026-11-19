package com.arovia.api.organization.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.arovia.api.organization.application.OrganizationMembershipView;

public record OrganizationMembershipResponse(
        UUID organizationId,
        String organizationName,
        String organizationStatus,
        String displayName,
        UUID membershipId,
        String membershipStatus,
        Instant joinedAt,
        List<String> roles
) {

    public static OrganizationMembershipResponse from(
            OrganizationMembershipView view
    ) {
        return new OrganizationMembershipResponse(
                view.organizationId(),
                view.organizationName(),
                view.organizationStatus(),
                view.displayName(),
                view.membershipId(),
                view.membershipStatus(),
                view.joinedAt(),
                view.roles()
        );
    }
}