package com.arovia.api.identity.application;

public class PasswordResetRateLimitedException
        extends RuntimeException {

    public PasswordResetRateLimitedException() {
        super("Too many password reset attempts. Please try again later.");
    }
}