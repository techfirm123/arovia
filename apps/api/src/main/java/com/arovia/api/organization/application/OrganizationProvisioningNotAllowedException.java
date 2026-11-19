
package com.arovia.api.organization.application;

public class OrganizationProvisioningNotAllowedException
        extends RuntimeException {

    public OrganizationProvisioningNotAllowedException(String message) {
        super(message);
    }
}
