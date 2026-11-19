package com.arovia.api.organization.web;

import java.io.IOException;
import java.util.UUID;

import com.arovia.api.identity.application.IdentityAuthenticatedUser;
import com.arovia.api.organization.application.OrganizationContext;
import com.arovia.api.organization.application.OrganizationContextResolver;
import com.arovia.api.organization.application.OrganizationMembershipNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OrganizationContextInterceptor
        implements HandlerInterceptor {

    public static final String REQUEST_ATTRIBUTE =
            OrganizationContext.class.getName();

    private static final String ORGANIZATION_SESSION_ATTRIBUTE =
            OrganizationSessionAttributes.ACTIVE_ORGANIZATION_ID;

    private final OrganizationContextResolver contextResolver;

    public OrganizationContextInterceptor(
            OrganizationContextResolver contextResolver
    ) {
        this.contextResolver = contextResolver;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!requiresOrganizationContext(handlerMethod)) {
            return true;
        }

        IdentityAuthenticatedUser authenticatedUser =
                getAuthenticatedUser();

        if (authenticatedUser == null) {
            writeError(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED",
                    "Authentication is required."
            );
            return false;
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            writeError(
                    response,
                    HttpStatus.CONFLICT,
                    "ORGANIZATION_CONTEXT_REQUIRED",
                    "Select an organization before continuing."
            );
            return false;
        }

        Object selectedOrganization =
                session.getAttribute(ORGANIZATION_SESSION_ATTRIBUTE);

        if (!(selectedOrganization instanceof UUID organizationId)) {
            writeError(
                    response,
                    HttpStatus.CONFLICT,
                    "ORGANIZATION_CONTEXT_REQUIRED",
                    "Select an organization before continuing."
            );
            return false;
        }

        try {
            OrganizationContext context = contextResolver.resolve(
                    authenticatedUser.getUserAccountId(),
                    organizationId
            );

            request.setAttribute(REQUEST_ATTRIBUTE, context);

            return true;

        } catch (OrganizationMembershipNotFoundException exception) {

            /*
             * The session selection is stale or the user's access
             * has been suspended or removed.
             *
             * Clear the invalid selection so the user can choose
             * another organization.
             */
            session.removeAttribute(ORGANIZATION_SESSION_ATTRIBUTE);

            writeError(
                    response,
                    HttpStatus.FORBIDDEN,
                    "ORGANIZATION_ACCESS_REVOKED",
                    "You no longer have access to the selected organization."
            );

            return false;
        }
    }

    private boolean requiresOrganizationContext(
            HandlerMethod handlerMethod
    ) {
        return AnnotatedElementUtils.hasAnnotation(
                handlerMethod.getMethod(),
                OrganizationScoped.class
        ) || AnnotatedElementUtils.hasAnnotation(
                handlerMethod.getBeanType(),
                OrganizationScoped.class
        );
    }

    private IdentityAuthenticatedUser getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof IdentityAuthenticatedUser user) {
            return user;
        }

        return null;
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {

        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        response.getWriter().write(
                """
                {
                  "status": %d,
                  "code": "%s",
                  "message": "%s"
                }
                """.formatted(
                        status.value(),
                        code,
                        message
                )
        );
    }
}