
package com.arovia.api.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmationRequest(

        @NotBlank
        @Size(min = 43, max = 128)
        String token

) {
}
