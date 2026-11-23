package com.arovia.api.customer.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.Locale;

import com.arovia.api.customer.application.CustomerCommand;
import com.arovia.api.customer.application.CustomerQueryPort;
import com.arovia.api.customer.domain.Customer;
import com.arovia.api.customer.domain.CustomerStatus;
import com.arovia.api.customer.domain.CustomerType;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCustomerQueryAdapter implements CustomerQueryPort {

    private static final RowMapper<Customer> CUSTOMER_ROW_MAPPER =
            JdbcCustomerQueryAdapter::mapCustomer;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCustomerQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String nextCustomerNumber(UUID organizationId) {
        Long number = jdbcTemplate.queryForObject(
                """
                INSERT INTO customer_number_sequence (
                    organization_id,
                    next_number
                )
                VALUES (?, 2)
                ON CONFLICT (organization_id)
                DO UPDATE SET
                    next_number = customer_number_sequence.next_number + 1
                RETURNING next_number - 1
                """,
                Long.class,
                organizationId
        );

        if (number == null) {
            throw new IllegalStateException(
                    "Unable to allocate a customer number."
            );
        }

        return "CUS-%06d".formatted(number);
    }

    @Override
    public void create(Customer customer) {
        jdbcTemplate.update(
                """
                INSERT INTO customer (
                    id,
                    organization_id,
                    customer_number,
                    customer_type,
                    display_name,
                    legal_name,
                    email,
                    phone,
                    notes,
                    status,
                    created_by_user_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                customer.id(),
                customer.organizationId(),
                customer.customerNumber(),
                customer.customerType().name(),
                customer.displayName(),
                customer.legalName(),
                customer.email(),
                customer.phone(),
                customer.notes(),
                customer.status().name(),
                customer.createdByUserId()
        );
    }

    @Override
    public Optional<Customer> findById(
            UUID organizationId,
            UUID customerId
    ) {
        List<Customer> customers = jdbcTemplate.query(
                """
                SELECT *
                FROM customer
                WHERE organization_id = ?
                  AND id = ?
                """,
                CUSTOMER_ROW_MAPPER,
                organizationId,
                customerId
        );

        return customers.stream().findFirst();
    }

    @Override
    public List<Customer> findPage(
            UUID organizationId,
            String search,
            int limit,
            int offset
    ) {
        String pattern = search == null
                ? null
                : "%" + search.toLowerCase() + "%";

        return jdbcTemplate.query(
                """
                SELECT *
                FROM customer
                WHERE organization_id = ?
                  AND status = 'ACTIVE'
                  AND (
                               CAST(? AS TEXT) IS NULL
                               OR lower(customer_number) LIKE ?
                               OR lower(display_name) LIKE ?
                               OR lower(COALESCE(legal_name, '')) LIKE ?
                               OR lower(COALESCE(email, '')) LIKE ?
                               OR lower(COALESCE(phone, '')) LIKE ?
                           )
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                OFFSET ?
                """,
                CUSTOMER_ROW_MAPPER,
                organizationId,
                pattern,
                pattern,
                pattern,
                pattern,
                pattern,
                pattern,
                limit,
                offset
        );
    }

    @Override
    public long count(UUID organizationId, String search) {
        String pattern = search == null
                ? null
                : "%" + search.toLowerCase() + "%";

        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM customer
                WHERE organization_id = ?
                  AND status = 'ACTIVE'
                  AND (
                               CAST(? AS TEXT) IS NULL
                               OR lower(customer_number) LIKE ?
                               OR lower(display_name) LIKE ?
                               OR lower(COALESCE(legal_name, '')) LIKE ?
                               OR lower(COALESCE(email, '')) LIKE ?
                               OR lower(COALESCE(phone, '')) LIKE ?
                           )
                """,
                Long.class,
                organizationId,
                pattern,
                pattern,
                pattern,
                pattern,
                pattern,
                pattern
        );

        return count == null ? 0 : count;
    }

    @Override
    public boolean update(
            UUID organizationId,
            UUID customerId,
            CustomerCommand command
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE customer
                SET customer_type = ?,
                    display_name = ?,
                    legal_name = ?,
                    email = ?,
                    phone = ?,
                    notes = ?,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE organization_id = ?
                  AND id = ?
                  AND status = 'ACTIVE'
                """,
                command.customerType().name(),
                command.displayName().trim(),
                clean(command.legalName()),
                normalizeEmail(command.email()),
                clean(command.phone()),
                clean(command.notes()),
                organizationId,
                customerId
        );

        return updated == 1;
    }

    @Override
    public boolean archive(
            UUID organizationId,
            UUID customerId
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE customer
                SET status = 'ARCHIVED',
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE organization_id = ?
                  AND id = ?
                  AND status = 'ACTIVE'
                """,
                organizationId,
                customerId
        );

        return updated == 1;
    }

    private static Customer mapCustomer(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new Customer(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("organization_id", UUID.class),
                resultSet.getString("customer_number"),
                CustomerType.valueOf(
                        resultSet.getString("customer_type")
                ),
                resultSet.getString("display_name"),
                resultSet.getString("legal_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("notes"),
                CustomerStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getObject("created_by_user_id", UUID.class),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getLong("version")
        );
    }

    private String normalizeEmail(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}