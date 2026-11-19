
package com.arovia.api.organization.application;

public record CreateOrganizationCommand(
        String name,
        OrganizationProfileInput profile,
        OrganizationLocationInput initialLocation
) {

    public record OrganizationProfileInput(
            String displayName,
            String legalName,
            String countryCode,
            String defaultCurrency,
            String timezone,
            String websiteUrl,
            String contactEmail,
            String contactPhone
    ) {
    }

    public record OrganizationLocationInput(
            String name,
            String locationCode,
            String locationType,
            String addressLine1,
            String addressLine2,
            String city,
            String stateOrRegion,
            String postalCode,
            String countryCode,
            String timezone
    ) {
    }
}
