package com.arovia.api.customer.web;

import java.util.UUID;

import com.arovia.api.customer.application.CustomerService;
import com.arovia.api.identity.application.IdentityAuthenticatedUser;
import com.arovia.api.organization.application.OrganizationContext;
import com.arovia.api.organization.web.OrganizationContextInterceptor;
import com.arovia.api.organization.web.OrganizationScoped;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@OrganizationScoped
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @RequestAttribute(
                    OrganizationContextInterceptor.REQUEST_ATTRIBUTE
            )
            OrganizationContext organizationContext,

            @AuthenticationPrincipal
            IdentityAuthenticatedUser authenticatedUser,

            @Valid @RequestBody
            CreateOrUpdateCustomerRequest request
    ) {
        var customer = customerService.create(
                organizationContext.organizationId(),
                authenticatedUser.getUserAccountId(),
                request.toCommand()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customer));
    }

    @GetMapping
    public ResponseEntity<CustomerPageResponse> list(
            @RequestAttribute(
                    OrganizationContextInterceptor.REQUEST_ATTRIBUTE
            )
            OrganizationContext organizationContext,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        var result = customerService.findPage(
                organizationContext.organizationId(),
                search,
                page,
                size
        );

        return ResponseEntity.ok(
                CustomerPageResponse.from(result)
        );
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> get(
            @RequestAttribute(
                    OrganizationContextInterceptor.REQUEST_ATTRIBUTE
            )
            OrganizationContext organizationContext,

            @PathVariable UUID customerId
    ) {
        var customer = customerService.get(
                organizationContext.organizationId(),
                customerId
        );

        return ResponseEntity.ok(
                CustomerResponse.from(customer)
        );
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> update(
            @RequestAttribute(
                    OrganizationContextInterceptor.REQUEST_ATTRIBUTE
            )
            OrganizationContext organizationContext,

            @PathVariable UUID customerId,

            @Valid @RequestBody
            CreateOrUpdateCustomerRequest request
    ) {
        var customer = customerService.update(
                organizationContext.organizationId(),
                customerId,
                request.toCommand()
        );

        return ResponseEntity.ok(
                CustomerResponse.from(customer)
        );
    }

    @PostMapping("/{customerId}/archive")
    public ResponseEntity<Void> archive(
            @RequestAttribute(
                    OrganizationContextInterceptor.REQUEST_ATTRIBUTE
            )
            OrganizationContext organizationContext,

            @PathVariable UUID customerId
    ) {
        customerService.archive(
                organizationContext.organizationId(),
                customerId
        );

        return ResponseEntity.noContent().build();
    }
}