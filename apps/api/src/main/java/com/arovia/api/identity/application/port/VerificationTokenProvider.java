package com.arovia.api.identity.application.port;

/**
 * Provides secure token generation and one-way token hashing
 * for Identity application use cases.
 */
public interface VerificationTokenProvider {

    String generateToken();

    String hashToken(String rawToken);
}