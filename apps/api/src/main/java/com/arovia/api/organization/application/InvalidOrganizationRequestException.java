
package com.arovia.api.organization.application;

public class InvalidOrganizationRequestException
        extends RuntimeException {

    public InvalidOrganizationRequestException(String message) {
        super(message);
    }
}
