package com.arovia.api.customer.web;

import com.arovia.api.customer.application.CustomerCommand;
import com.arovia.api.customer.domain.CustomerType;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrUpdateCustomerRequest(
        @NotNull
        CustomerType customerType,

        @NotBlank
        @Size(max = 160)
        String displayName,

        @Size(max = 200)
        String legalName,


        @Size(max = 320)
        String email,

        @Size(max = 40)
        String phone,

        @Size(max = 2000)
        String notes
) {
    public CustomerCommand toCommand() {
        return new CustomerCommand(
                customerType,
                displayName,
                legalName,
                email,
                phone,
                notes
        );
    }
}