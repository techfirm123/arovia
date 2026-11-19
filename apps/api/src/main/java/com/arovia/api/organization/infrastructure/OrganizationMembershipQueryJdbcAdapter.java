package com.arovia.api.organization.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.arovia.api.organization.application.OrganizationMembershipView;
import com.arovia.api.organization.application.port.OrganizationMembershipQueryPort;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationMembershipQueryJdbcAdapter
        implements OrganizationMembershipQueryPort {

    private static final String BASE_QUERY = """
            SELECT
                o.id AS organization_id,
                o.name AS organization_name,
                o.status AS organization_status,
                op.display_name AS display_name,
                m.id AS membership_id,
                m.status AS membership_status,
                m.joined_at AS joined_at,
                COALESCE(
                    STRING_AGG(r.role_code, ',' ORDER BY r.role_code),
                    ''
                ) AS roles
            FROM organization_membership m
            INNER JOIN organization o
                ON o.id = m.organization_id
            LEFT JOIN organization_profile op
                ON op.organization_id = o.id
            LEFT JOIN organization_membership_role r
                ON r.membership_id = m.id
            WHERE m.user_account_id = ?
              AND m.status = 'ACTIVE'
              AND o.status = 'ACTIVE'
            """;

    private static final String GROUP_AND_ORDER = """
            GROUP BY
                o.id,
                o.name,
                o.status,
                op.display_name,
                m.id,
                m.status,
                m.joined_at
            ORDER BY o.name ASC
            """;

    private static final RowMapper<OrganizationMembershipView> ROW_MAPPER =
            OrganizationMembershipQueryJdbcAdapter::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public OrganizationMembershipQueryJdbcAdapter(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OrganizationMembershipView> findActiveOrganizations(
            UUID userAccountId
    ) {
        String sql = BASE_QUERY + GROUP_AND_ORDER;

        return jdbcTemplate.query(
                sql,
                ROW_MAPPER,
                userAccountId
        );
    }

    @Override
    public Optional<OrganizationMembershipView> findActiveOrganization(
            UUID userAccountId,
            UUID organizationId
    ) {
        String sql = BASE_QUERY
                + " AND o.id = ? "
                + GROUP_AND_ORDER;

        List<OrganizationMembershipView> results = jdbcTemplate.query(
                sql,
                ROW_MAPPER,
                userAccountId,
                organizationId
        );

        return results.stream().findFirst();
    }

    private static OrganizationMembershipView mapRow(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {

        Timestamp joinedAt = resultSet.getTimestamp("joined_at");

        String rolesValue = resultSet.getString("roles");

        List<String> roles = rolesValue == null || rolesValue.isBlank()
                ? List.of()
                : Arrays.asList(rolesValue.split(","));

        return new OrganizationMembershipView(
                resultSet.getObject("organization_id", UUID.class),
                resultSet.getString("organization_name"),
                resultSet.getString("organization_status"),
                resultSet.getString("display_name"),
                resultSet.getObject("membership_id", UUID.class),
                resultSet.getString("membership_status"),
                joinedAt.toInstant(),
                roles
        );
    }
}