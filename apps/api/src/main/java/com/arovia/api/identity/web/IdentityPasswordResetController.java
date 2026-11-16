package com.arovia.api.identity.web;

import com.arovia.api.identity.application.IdentityPasswordResetService;
import com.arovia.api.identity.application.InvalidPasswordResetTokenException;
import com.arovia.api.identity.application.PasswordPolicyException;
import com.arovia.api.identity.application.PasswordResetRateLimitedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity/password-resets")
public class IdentityPasswordResetController {

    private final IdentityPasswordResetService passwordResetService;

    public IdentityPasswordResetController(
            IdentityPasswordResetService passwordResetService
    ) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping
    public ResponseEntity<PasswordResetAcceptedResponse> requestReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest servletRequest
    ) {
        passwordResetService.requestReset(
                request.email(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(PasswordResetAcceptedResponse.generic());
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmReset(
            @Valid @RequestBody
            PasswordResetConfirmationRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            passwordResetService.confirmReset(
                    request.token(),
                    request.newPassword(),
                    servletRequest.getRemoteAddr(),
                    servletRequest.getHeader("User-Agent")
            );

            return ResponseEntity.noContent().build();

        } catch (InvalidPasswordResetTokenException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(new PasswordResetErrorResponse(
                            "This password reset link is invalid, expired, "
                                    + "or has already been used."
                    ));

        } catch (PasswordPolicyException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(new PasswordResetErrorResponse(
                            "Choose a password that meets Arovia's "
                                    + "password requirements."
                    ));

        } catch (PasswordResetRateLimitedException exception) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new PasswordResetErrorResponse(
                            "Too many reset attempts. Please try again later."
                    ));
        }
    }
}