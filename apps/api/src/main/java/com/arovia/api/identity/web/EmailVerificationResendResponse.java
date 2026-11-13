
package com.arovia.api.identity.web;

public record EmailVerificationResendResponse(String message) {

    public static EmailVerificationResendResponse generic() {
        return new EmailVerificationResendResponse(
                "If the account requires verification, "
                        + "instructions will be sent."
        );
    }
}
