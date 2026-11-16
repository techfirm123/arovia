package com.arovia.api.identity.infrastructure;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class AbsoluteSessionLifetimeFilter
        extends OncePerRequestFilter {

    public static final String AUTHENTICATED_AT_ATTRIBUTE =
            "arovia.identity.authenticated-at";

    private final Clock clock;
    private final Duration maximumLifetime;

    public AbsoluteSessionLifetimeFilter(
            Clock clock,
            Duration maximumLifetime
    ) {
        if (maximumLifetime == null
                || maximumLifetime.isZero()
                || maximumLifetime.isNegative()) {
            throw new IllegalArgumentException(
                    "Absolute session lifetime must be positive."
            );
        }

        this.clock = clock;
        this.maximumLifetime = maximumLifetime;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!authenticated) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            expireAuthentication(response);
            return;
        }

        Object authenticatedAtValue;

        try {
            authenticatedAtValue = session.getAttribute(
                    AUTHENTICATED_AT_ATTRIBUTE
            );
        } catch (IllegalStateException exception) {
            expireAuthentication(response);
            return;
        }

        if (!(authenticatedAtValue instanceof Long authenticatedAtMillis)) {
            expireSession(session, response);
            return;
        }

        Instant authenticatedAt = Instant.ofEpochMilli(
                authenticatedAtMillis
        );

        Duration sessionAge = Duration.between(
                authenticatedAt,
                clock.instant()
        );

        if (sessionAge.isNegative()
                || sessionAge.compareTo(maximumLifetime) >= 0) {
            expireSession(session, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void expireSession(
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // The session may already have expired.
        }

        expireAuthentication(response);
    }

    private void expireAuthentication(
            HttpServletResponse response
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.sendError(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication session expired."
        );
    }
}