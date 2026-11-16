package com.arovia.api.identity.infrastructure;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class IdentityUserDetailsService implements UserDetailsService {

    private static final String USER_NOT_FOUND_MESSAGE =
            "Invalid email address or password.";

    private final JdbcTemplate jdbcTemplate;

    public IdentityUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String suppliedEmail) {
        if (suppliedEmail == null || suppliedEmail.isBlank()) {
            throw new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE);
        }

        String normalizedEmail = suppliedEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        List<IdentityUserPrincipal> users = jdbcTemplate.query(
                """
                SELECT
                    id,
                    email,
                    normalized_email,
                    display_name,
                    password_hash,
                    status,
                    email_verified_at
                FROM user_account
                WHERE normalized_email = ?
                """,
                (resultSet, rowNumber) -> new IdentityUserPrincipal(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("normalized_email"),
                        resultSet.getString("display_name"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("status"),
                        resultSet.getTimestamp("email_verified_at") != null
                ),
                normalizedEmail
        );

        if (users.isEmpty()) {
            throw new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE);
        }

        return users.get(0);
    }
}