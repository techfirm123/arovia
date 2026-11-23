package com.arovia.api.customer.web;

import java.time.Instant;
import java.util.UUID;

import com.arovia.api.customer.domain.Customer;
import com.arovia.api.customer.domain.CustomerStatus;
import com.arovia.api.customer.domain.CustomerType;

public record CustomerResponse(
        UUID id,
        String customerNumber,
        CustomerType customerType,
        String displayName,
        String legalName,
        String email,
        String phone,
        String notes,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id(),
                customer.customerNumber(),
                customer.customerType(),
                customer.displayName(),
                customer.legalName(),
                customer.email(),
                customer.phone(),
                customer.notes(),
                customer.status(),
                customer.createdAt(),
                customer.updatedAt()
        );
    }
}