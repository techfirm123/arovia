package com.arovia.api.customer.application;

import com.arovia.api.customer.domain.CustomerType;

public record CustomerCommand(
        CustomerType customerType,
        String displayName,
        String legalName,
        String email,
        String phone,
        String notes
) {
}