package com.arovia.api.identity.application;

import com.arovia.api.identity.application.port.IdentityEmailVerificationPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;

import org.springframework.stereotype.Service;

@Service
public class IdentityEmailVerificationService {

    private final IdentityEmailVerificationPort verificationPort;
    private final VerificationTokenProvider tokenProvider;

    public IdentityEmailVerificationService(
            IdentityEmailVerificationPort verificationPort,
            VerificationTokenProvider tokenProvider
    ) {
        this.verificationPort = verificationPort;
        this.tokenProvider = tokenProvider;
    }

    public void confirm(
            String rawToken,
            String sourceIp,
            String userAgent
    ) {
        String tokenHash = tokenProvider.hashToken(rawToken);

        verificationPort.activateAccount(
                tokenHash,
                sourceIp,
                userAgent
        );
    }
}