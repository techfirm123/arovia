
package com.arovia.api.organization.web;

import com.arovia.api.organization.application.CreateOrganizationCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @Valid
        OrganizationProfileRequest profile,

        @Valid
        OrganizationLocationRequest initialLocation
) {

    public CreateOrganizationCommand toCommand() {
        CreateOrganizationCommand.OrganizationProfileInput profileInput =
                profile == null ? null : profile.toInput();

        CreateOrganizationCommand.OrganizationLocationInput locationInput =
                initialLocation == null
                        ? null
                        : initialLocation.toInput();

        return new CreateOrganizationCommand(
                name,
                profileInput,
                locationInput
        );
    }

    public record OrganizationProfileRequest(
            @Size(max = 160)
            String displayName,

            @Size(max = 200)
            String legalName,

            @Pattern(regexp = "^[A-Za-z]{2}$")
            String countryCode,

            @Pattern(regexp = "^[A-Za-z]{3}$")
            String defaultCurrency,

            @Size(max = 64)
            String timezone,

            @Size(max = 500)
            String websiteUrl,

            @Email
            @Size(max = 320)
            String contactEmail,

            @Size(max = 40)
            String contactPhone
    ) {
        private CreateOrganizationCommand.OrganizationProfileInput toInput() {
            return new CreateOrganizationCommand.OrganizationProfileInput(
                    displayName,
                    legalName,
                    countryCode,
                    defaultCurrency,
                    timezone,
                    websiteUrl,
                    contactEmail,
                    contactPhone
            );
        }
    }

    public record OrganizationLocationRequest(
            @NotBlank
            @Size(max = 120)
            String name,

            @Size(max = 40)
            String locationCode,

            @Pattern(
                    regexp = "^(BUSINESS|WAREHOUSE|SERVICE_CENTER|OFFICE)?$"
            )
            String locationType,

            @Size(max = 200)
            String addressLine1,

            @Size(max = 200)
            String addressLine2,

            @Size(max = 120)
            String city,

            @Size(max = 120)
            String stateOrRegion,

            @Size(max = 24)
            String postalCode,

            @Pattern(regexp = "^[A-Za-z]{2}$")
            String countryCode,

            @Size(max = 64)
            String timezone
    ) {
        private CreateOrganizationCommand.OrganizationLocationInput toInput() {
            return new CreateOrganizationCommand.OrganizationLocationInput(
                    name,
                    locationCode,
                    locationType,
                    addressLine1,
                    addressLine2,
                    city,
                    stateOrRegion,
                    postalCode,
                    countryCode,
                    timezone
            );
        }
    }
}
