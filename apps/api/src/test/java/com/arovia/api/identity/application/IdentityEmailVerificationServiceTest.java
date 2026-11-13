
package com.arovia.api.identity.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.arovia.api.identity.infrastructure.IdentityEmailVerificationStore;
import com.arovia.api.identity.infrastructure.VerificationTokenGenerator;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

class IdentityEmailVerificationServiceTest {

    @Test
    void hashesSubmittedTokenBeforeCallingStore() {
        IdentityEmailVerificationStore store =
                Mockito.mock(IdentityEmailVerificationStore.class);

        IdentityEmailVerificationService service =
                new IdentityEmailVerificationService(
                        store,
                        new VerificationTokenGenerator(
                                new java.security.SecureRandom()
                        )
                );

        String rawToken =
                "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

        service.confirm(
                rawToken,
                "127.0.0.1",
                "Arovia test client"
        );

        String expectedHash =
                new VerificationTokenGenerator(
                        new java.security.SecureRandom()
                ).hashToken(rawToken);

        verify(store).activateAccount(
                expectedHash,
                "127.0.0.1",
                "Arovia test client"
        );

        verifyNoMoreInteractions(store);
    }
}
