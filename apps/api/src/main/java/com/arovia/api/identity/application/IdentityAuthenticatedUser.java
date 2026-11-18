package com.arovia.api.identity.application;

import java.util.UUID;

/**
 * Application-facing view of the currently authenticated identity.
 *
 * Web controllers depend on this contract rather than on the
 * Spring Security principal implementation.
 */
public interface IdentityAuthenticatedUser {

    UUID getUserAccountId();

    String getEmail();

    String getDisplayName();

    boolean isEmailVerified();
}