package com.arovia.api.organization.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.organization.application.OrganizationMembershipView;

public interface OrganizationMembershipQueryPort {

    List<OrganizationMembershipView> findActiveOrganizations(
            UUID userAccountId
    );

    Optional<OrganizationMembershipView> findActiveOrganization(
            UUID userAccountId,
            UUID organizationId
    );
}