
package com.arovia.api.organization.application;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

import com.arovia.api.organization.application.port.OrganizationProvisioningPort;

@Service
public class OrganizationProvisioningService {

    private final OrganizationProvisioningPort provisioningPort;

    public OrganizationProvisioningService(
            OrganizationProvisioningPort provisioningPort
    ) {
        this.provisioningPort = Objects.requireNonNull(
                provisioningPort,
                "provisioningPort must not be null"
        );
    }

    public OrganizationProvisioningResult createOrganization(
            UUID authenticatedUserId,
            boolean emailVerified,
            CreateOrganizationCommand command
    ) {
        Objects.requireNonNull(
                authenticatedUserId,
                "authenticatedUserId must not be null"
        );
        Objects.requireNonNull(command, "command must not be null");

        if (!emailVerified) {
            throw new OrganizationProvisioningNotAllowedException(
                    "Email verification is required before creating an organization."
            );
        }

        String name = command.name() == null
                ? ""
                : command.name().trim();

        if (name.isBlank() || name.length() > 160) {
            throw new InvalidOrganizationRequestException(
                    "Organization name must contain between 1 and 160 characters."
            );
        }

        CreateOrganizationCommand normalizedCommand =
                new CreateOrganizationCommand(
                        name,
                        command.profile(),
                        command.initialLocation()
                );

        return provisioningPort.provision(
                authenticatedUserId,
                normalizedCommand
        );
    }
}
