package com.arovia.api.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmationRequest(

        @NotBlank
        @Size(max = 128)
        String token,

        @NotBlank
        @Size(max = 512)
        String newPassword

) {
}