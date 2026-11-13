
package com.arovia.api.identity.web;

public record RegistrationAcceptedResponse(String message) {

    public static RegistrationAcceptedResponse generic() {
        return new RegistrationAcceptedResponse(
                "If an account can be created for this address, "
                        + "you will receive an email with the next steps."
        );
    }
}
