package com.arovia.api.organization.application;

public class OrganizationMembershipNotFoundException
        extends RuntimeException {

    public OrganizationMembershipNotFoundException() {
        super("The requested active organization membership was not found.");
    }
}