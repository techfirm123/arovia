package com.arovia.api.identity.web;

public record PasswordResetAcceptedResponse(
        String message
) {

    public static PasswordResetAcceptedResponse generic() {
        return new PasswordResetAcceptedResponse(
                "If an account can be recovered for this address, "
                        + "you will receive password reset instructions."
        );
    }
}