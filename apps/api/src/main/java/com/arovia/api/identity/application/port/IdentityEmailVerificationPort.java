package com.arovia.api.identity.application.port;

public interface IdentityEmailVerificationPort {

    void activateAccount(
            String tokenHash,
            String sourceIp,
            String userAgent
    );
}