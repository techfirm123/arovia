
package com.arovia.api.identity.web;

import com.arovia.api.identity.application.IdentityEmailVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityEmailVerificationController {

    private final IdentityEmailVerificationService verificationService;

    public IdentityEmailVerificationController(
            IdentityEmailVerificationService verificationService
    ) {
        this.verificationService = verificationService;
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirm(
            @Valid @RequestBody
            EmailVerificationConfirmationRequest request,
            HttpServletRequest servletRequest
    ) {
        verificationService.confirm(
                request.token(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.noContent().build();
    }
}
