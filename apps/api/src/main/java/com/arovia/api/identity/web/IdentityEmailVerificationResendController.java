package com.arovia.api.identity.web;

import com.arovia.api.identity.application.IdentityEmailVerificationResendService;
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
@RequestMapping("/api/v1/identity/email-verification")
public class IdentityEmailVerificationResendController {

    private final IdentityEmailVerificationResendService resendService;
    private final ClientAddressResolver clientAddressResolver;

    public IdentityEmailVerificationResendController(
            IdentityEmailVerificationResendService resendService,
            ClientAddressResolver clientAddressResolver
    ) {
        this.resendService = resendService;
        this.clientAddressResolver = clientAddressResolver;
    }

    @PostMapping("/resend")
    public ResponseEntity<EmailVerificationResendResponse> resend(
            @Valid @RequestBody EmailVerificationResendRequest request,
            HttpServletRequest servletRequest
    ) {
        resendService.resend(
                request.email(),
                clientAddressResolver.resolve(servletRequest),
                servletRequest.getHeader("User-Agent")
        );

        /*
         * Always return the same response regardless of whether:
         * - the account exists,
         * - the account is pending,
         * - the account is already verified,
         * - the account is inside the cooldown,
         * - a rate limit was exceeded.
         */
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(EmailVerificationResendResponse.generic());
    }
}