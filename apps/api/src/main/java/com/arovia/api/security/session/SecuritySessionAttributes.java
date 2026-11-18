package com.arovia.api.security.session;

public final class SecuritySessionAttributes {

    /*
     * Keep the existing session attribute name so that sessions created
     * by the previous application version remain readable during rollout.
     */
    public static final String AUTHENTICATED_AT =
            "arovia.identity.authenticated-at";

    private SecuritySessionAttributes() {
        throw new UnsupportedOperationException(
                "This class must not be instantiated."
        );
    }
}