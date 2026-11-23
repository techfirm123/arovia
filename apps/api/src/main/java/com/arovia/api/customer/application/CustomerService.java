package com.arovia.api.customer.application;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.arovia.api.customer.domain.Customer;
import com.arovia.api.customer.domain.CustomerStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 100;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final CustomerQueryPort customerQueryPort;

    public CustomerService(CustomerQueryPort customerQueryPort) {
        this.customerQueryPort = customerQueryPort;
    }

    @Transactional
    public Customer create(
            UUID organizationId,
            UUID actorUserId,
            CustomerCommand command
    ) {
        validate(command);

        String customerNumber =
                customerQueryPort.nextCustomerNumber(organizationId);

        Customer customer = new Customer(
                UUID.randomUUID(),
                organizationId,
                customerNumber,
                command.customerType(),
                cleanRequired(command.displayName()),
                clean(command.legalName()),
                normalizeEmail(command.email()),
                clean(command.phone()),
                clean(command.notes()),
                CustomerStatus.ACTIVE,
                actorUserId,
                null,
                null,
                0
        );

        customerQueryPort.create(customer);

        return customerQueryPort.findById(
                organizationId,
                customer.id()
        ).orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public CustomerPage findPage(
            UUID organizationId,
            String search,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new CustomerValidationException(
                    "Page must be zero or greater."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomerValidationException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE + "."
            );
        }

        String normalizedSearch = clean(search);

        if (normalizedSearch != null
                && normalizedSearch.length() > MAX_SEARCH_LENGTH) {
            throw new CustomerValidationException(
                    "Search text must not exceed "
                            + MAX_SEARCH_LENGTH + " characters."
            );
        }

        long offset = (long) page * size;

        if (offset > Integer.MAX_VALUE) {
            throw new CustomerValidationException(
                    "Requested page is outside the supported range."
            );
        }

        long total = customerQueryPort.count(
                organizationId,
                normalizedSearch
        );

        var items = customerQueryPort.findPage(
                organizationId,
                normalizedSearch,
                size,
                (int) offset
        );

        int totalPages = total == 0
                ? 0
                : (int) Math.ceil((double) total / size);

        return new CustomerPage(
                items,
                page,
                size,
                total,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public Customer get(
            UUID organizationId,
            UUID customerId
    ) {
        return customerQueryPort.findById(
                organizationId,
                customerId
        ).orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional
    public Customer update(
            UUID organizationId,
            UUID customerId,
            CustomerCommand command
    ) {
        validate(command);

        boolean updated = customerQueryPort.update(
                organizationId,
                customerId,
                command
        );

        if (!updated) {
            throw new CustomerNotFoundException();
        }

        return get(organizationId, customerId);
    }

    @Transactional
    public void archive(
            UUID organizationId,
            UUID customerId
    ) {
        if (!customerQueryPort.archive(organizationId, customerId)) {
            throw new CustomerNotFoundException();
        }
    }

    private void validate(CustomerCommand command) {
        if (command == null) {
            throw new CustomerValidationException(
                    "Customer details are required."
            );
        }

        if (command.customerType() == null) {
            throw new CustomerValidationException(
                    "Customer type is required."
            );
        }

        String displayName = clean(command.displayName());

        if (displayName == null) {
            throw new CustomerValidationException(
                    "Customer name is required."
            );
        }

        if (displayName.length() > 160) {
            throw new CustomerValidationException(
                    "Customer name must not exceed 160 characters."
            );
        }

        validateMaximum(command.legalName(), 200, "Legal name");
        validateMaximum(command.email(), 320, "Email");
        validateMaximum(command.phone(), 40, "Phone");
        validateMaximum(command.notes(), 2000, "Notes");

        String email = clean(command.email());

        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomerValidationException(
                    "Enter a valid email address."
            );
        }
    }

    private void validateMaximum(
            String value,
            int maximum,
            String field
    ) {
        if (value != null && value.trim().length() > maximum) {
            throw new CustomerValidationException(
                    field + " must not exceed " + maximum + " characters."
            );
        }
    }

    private String normalizeEmail(String value) {
        String cleaned = clean(value);
        return cleaned == null
                ? null
                : cleaned.toLowerCase(Locale.ROOT);
    }

    private String cleanRequired(String value) {
        return value.trim();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}