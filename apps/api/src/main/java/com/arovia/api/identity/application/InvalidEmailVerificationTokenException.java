package com.arovia.api.identity.application;

public class InvalidEmailVerificationTokenException
        extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super("The email verification link is invalid or has expired.");
    }
}