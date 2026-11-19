package com.arovia.api.organization.web;

import java.util.List;
import java.util.UUID;

import com.arovia.api.identity.application.IdentityAuthenticatedUser;
import com.arovia.api.organization.application.OrganizationMembershipService;
import com.arovia.api.organization.application.OrganizationProvisioningService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationProvisioningService provisioningService;
    private final OrganizationMembershipService membershipService;

    public OrganizationController(
            OrganizationProvisioningService provisioningService,
            OrganizationMembershipService membershipService
    ) {
        this.provisioningService = provisioningService;
        this.membershipService = membershipService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @AuthenticationPrincipal IdentityAuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        var result = provisioningService.createOrganization(
                authenticatedUser.getUserAccountId(),
                authenticatedUser.isEmailVerified(),
                request.toCommand()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(OrganizationResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationMembershipResponse>>
    getOrganizations(
            @AuthenticationPrincipal
            IdentityAuthenticatedUser authenticatedUser
    ) {

        var organizations = membershipService.getActiveOrganizations(
                authenticatedUser.getUserAccountId()
        );

        return ResponseEntity.ok(
                organizations.stream()
                        .map(OrganizationMembershipResponse::from)
                        .toList()
        );
    }

    @GetMapping("/active")
    public ResponseEntity<OrganizationMembershipResponse>
    getActiveOrganization(
            @AuthenticationPrincipal
            IdentityAuthenticatedUser authenticatedUser,
            HttpServletRequest request
    ) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return ResponseEntity.noContent().build();
        }

        Object selectedId = session.getAttribute(
                OrganizationSessionAttributes.ACTIVE_ORGANIZATION_ID
        );

        if (!(selectedId instanceof UUID organizationId)) {
            return ResponseEntity.noContent().build();
        }

        var organization = membershipService.getActiveOrganization(
                authenticatedUser.getUserAccountId(),
                organizationId
        );

        return ResponseEntity.ok(
                OrganizationMembershipResponse.from(organization)
        );
    }

    @PutMapping("/active")
    public ResponseEntity<OrganizationMembershipResponse>
    selectActiveOrganization(
            @AuthenticationPrincipal
            IdentityAuthenticatedUser authenticatedUser,
            @Valid @RequestBody
            SelectOrganizationRequest request,
            HttpServletRequest servletRequest
    ) {

        UUID userAccountId = authenticatedUser.getUserAccountId();

        /*
         * Validate active membership before storing the selection.
         * The submitted organization ID is never trusted by itself.
         */
        var organization = membershipService.getActiveOrganization(
                userAccountId,
                request.organizationId()
        );

        HttpSession session = servletRequest.getSession(true);

        session.setAttribute(
                OrganizationSessionAttributes.ACTIVE_ORGANIZATION_ID,
                organization.organizationId()
        );

        return ResponseEntity.ok(
                OrganizationMembershipResponse.from(organization)
        );
    }

    @DeleteMapping("/active")
    public ResponseEntity<Void> clearActiveOrganization(
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute(
                    OrganizationSessionAttributes.ACTIVE_ORGANIZATION_ID
            );
        }

        return ResponseEntity.noContent().build();
    }
}