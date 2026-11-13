
package com.arovia.api.identity.application;

import com.arovia.api.identity.infrastructure.IdentityEmailVerificationStore;
import com.arovia.api.identity.infrastructure.VerificationTokenGenerator;

import org.springframework.stereotype.Service;

@Service
public class IdentityEmailVerificationService {

    private final IdentityEmailVerificationStore verificationStore;
    private final VerificationTokenGenerator tokenGenerator;

    public IdentityEmailVerificationService(
            IdentityEmailVerificationStore verificationStore,
            VerificationTokenGenerator tokenGenerator
    ) {
        this.verificationStore = verificationStore;
        this.tokenGenerator = tokenGenerator;
    }

    public void confirm(
            String rawToken,
            String sourceIp,
            String userAgent
    ) {
        String tokenHash = tokenGenerator.hashToken(rawToken);

        verificationStore.activateAccount(
                tokenHash,
                sourceIp,
                userAgent
        );
    }
}
