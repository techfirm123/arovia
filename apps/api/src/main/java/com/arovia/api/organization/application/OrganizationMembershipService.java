package com.arovia.api.organization.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.arovia.api.organization.application.port.OrganizationMembershipQueryPort;

import org.springframework.stereotype.Service;

@Service
public class OrganizationMembershipService {

    private final OrganizationMembershipQueryPort queryPort;

    public OrganizationMembershipService(
            OrganizationMembershipQueryPort queryPort
    ) {
        this.queryPort = Objects.requireNonNull(
                queryPort,
                "queryPort must not be null"
        );
    }

    public List<OrganizationMembershipView> getActiveOrganizations(
            UUID userAccountId
    ) {
        Objects.requireNonNull(
                userAccountId,
                "userAccountId must not be null"
        );

        return queryPort.findActiveOrganizations(userAccountId);
    }

    public OrganizationMembershipView getActiveOrganization(
            UUID userAccountId,
            UUID organizationId
    ) {
        Objects.requireNonNull(
                userAccountId,
                "userAccountId must not be null"
        );
        Objects.requireNonNull(
                organizationId,
                "organizationId must not be null"
        );

        return queryPort.findActiveOrganization(
                userAccountId,
                organizationId
        ).orElseThrow(OrganizationMembershipNotFoundException::new);
    }
}