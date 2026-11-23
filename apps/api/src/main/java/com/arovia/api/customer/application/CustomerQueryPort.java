package com.arovia.api.customer.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.customer.domain.Customer;

public interface CustomerQueryPort {

    String nextCustomerNumber(UUID organizationId);

    void create(Customer customer);

    Optional<Customer> findById(
            UUID organizationId,
            UUID customerId
    );

    List<Customer> findPage(
            UUID organizationId,
            String search,
            int limit,
            int offset
    );

    long count(
            UUID organizationId,
            String search
    );

    boolean update(
            UUID organizationId,
            UUID customerId,
            CustomerCommand command
    );

    boolean archive(
            UUID organizationId,
            UUID customerId
    );
}