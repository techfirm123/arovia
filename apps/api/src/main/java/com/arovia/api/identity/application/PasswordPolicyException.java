package com.arovia.api.identity.application;

public class PasswordPolicyException extends RuntimeException {

    public PasswordPolicyException() {
        super("Password does not meet the account security requirements.");
    }
}