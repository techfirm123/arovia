
package com.arovia.api.organization.application.port;

import java.util.UUID;

import com.arovia.api.organization.application.CreateOrganizationCommand;
import com.arovia.api.organization.application.OrganizationProvisioningResult;

public interface OrganizationProvisioningPort {

    OrganizationProvisioningResult provision(
            UUID ownerUserAccountId,
            CreateOrganizationCommand command
    );
}
