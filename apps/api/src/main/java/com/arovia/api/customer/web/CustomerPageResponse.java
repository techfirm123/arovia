package com.arovia.api.customer.web;

import java.util.List;

import com.arovia.api.customer.application.CustomerPage;

public record CustomerPageResponse(
        List<CustomerResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public CustomerPageResponse {
        items = List.copyOf(items);
    }

    public static CustomerPageResponse from(CustomerPage page) {
        return new CustomerPageResponse(
                page.items().stream()
                        .map(CustomerResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}