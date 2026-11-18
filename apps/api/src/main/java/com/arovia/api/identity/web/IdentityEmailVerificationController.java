package com.arovia.api.identity.web;

import com.arovia.api.identity.application.IdentityEmailVerificationService;
import com.arovia.api.security.client.ClientAddressResolver;

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
    private final ClientAddressResolver clientAddressResolver;

    public IdentityEmailVerificationController(
            IdentityEmailVerificationService verificationService,
            ClientAddressResolver clientAddressResolver
    ) {
        this.verificationService = verificationService;
        this.clientAddressResolver = clientAddressResolver;
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirm(
            @Valid @RequestBody
            EmailVerificationConfirmationRequest request,
            HttpServletRequest servletRequest
    ) {
        verificationService.confirm(
                request.token(),
                clientAddressResolver.resolve(servletRequest),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.noContent().build();
    }
}