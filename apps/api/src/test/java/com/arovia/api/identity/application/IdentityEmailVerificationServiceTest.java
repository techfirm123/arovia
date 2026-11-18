package com.arovia.api.identity.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.arovia.api.identity.application.port.IdentityEmailVerificationPort;
import com.arovia.api.identity.application.port.VerificationTokenProvider;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

class IdentityEmailVerificationServiceTest {

    @Test
    void hashesSubmittedTokenBeforeCallingPort() {
        IdentityEmailVerificationPort verificationPort =
                Mockito.mock(IdentityEmailVerificationPort.class);

        VerificationTokenProvider tokenProvider =
                Mockito.mock(VerificationTokenProvider.class);

        String rawToken =
                "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

        when(tokenProvider.hashToken(rawToken))
                .thenReturn("expected-token-hash");

        IdentityEmailVerificationService service =
                new IdentityEmailVerificationService(
                        verificationPort,
                        tokenProvider
                );

        service.confirm(
                rawToken,
                "127.0.0.1",
                "Arovia test client"
        );

        verify(tokenProvider).hashToken(rawToken);

        verify(verificationPort).activateAccount(
                "expected-token-hash",
                "127.0.0.1",
                "Arovia test client"
        );

        verifyNoMoreInteractions(
                tokenProvider,
                verificationPort
        );
    }
}