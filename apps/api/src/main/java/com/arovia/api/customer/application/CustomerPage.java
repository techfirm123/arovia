package com.arovia.api.customer.application;

import java.util.List;

import com.arovia.api.customer.domain.Customer;

public record CustomerPage(
        List<Customer> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public CustomerPage {
        items = List.copyOf(items);
    }
}