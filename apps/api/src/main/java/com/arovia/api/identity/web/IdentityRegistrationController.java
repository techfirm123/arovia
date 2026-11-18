package com.arovia.api.identity.web;

import com.arovia.api.identity.application.IdentityRegistrationService;
import com.arovia.api.security.client.ClientAddressResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityRegistrationController {

    private final IdentityRegistrationService registrationService;
    private final ClientAddressResolver clientAddressResolver;

    public IdentityRegistrationController(
            IdentityRegistrationService registrationService,
            ClientAddressResolver clientAddressResolver
    ) {
        this.registrationService = registrationService;
        this.clientAddressResolver = clientAddressResolver;
    }

    @PostMapping("/registrations")
    public ResponseEntity<RegistrationAcceptedResponse> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest servletRequest
    ) {
        registrationService.register(
                request.displayName(),
                request.email(),
                request.password(),
                clientAddressResolver.resolve(servletRequest),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(RegistrationAcceptedResponse.generic());
    }
}