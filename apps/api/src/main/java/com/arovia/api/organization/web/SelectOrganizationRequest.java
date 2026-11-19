package com.arovia.api.organization.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SelectOrganizationRequest(
        @NotNull UUID organizationId
) {
}