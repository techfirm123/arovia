package com.arovia.api.organization.web;

public final class OrganizationSessionAttributes {

    public static final String ACTIVE_ORGANIZATION_ID =
            "arovia.organization.active-id";

    private OrganizationSessionAttributes() {
        throw new UnsupportedOperationException(
                "This class must not be instantiated."
        );
    }
}