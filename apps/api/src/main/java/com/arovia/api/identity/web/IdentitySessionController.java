package com.arovia.api.identity.web;

import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;

import com.arovia.api.identity.application.IdentityAuthenticatedUser;
import com.arovia.api.identity.application.IdentityLoginRateLimiter;
import com.arovia.api.identity.application.port.IdentityAuthenticationAuditPort;
import com.arovia.api.identity.domain.IdentityAuditEventType;
import com.arovia.api.security.client.ClientAddressResolver;
import com.arovia.api.security.session.SecuritySessionAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentitySessionController {

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Unable to sign in with the supplied credentials.";

    private static final String LOGIN_THROTTLED_MESSAGE =
            "Unable to sign in at this time. Please try again later.";

    private static final String USER_AGENT_HEADER = "User-Agent";

    private final AuthenticationManager authenticationManager;
    private final IdentityLoginRateLimiter loginRateLimiter;
    private final IdentityAuthenticationAuditPort auditPort;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final ClientAddressResolver clientAddressResolver;
    private final Clock clock;

    public IdentitySessionController(
            AuthenticationManager authenticationManager,
            IdentityLoginRateLimiter loginRateLimiter,
            IdentityAuthenticationAuditPort auditPort,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            ClientAddressResolver clientAddressResolver,
            Clock applicationClock
    ) {
        this.authenticationManager = authenticationManager;
        this.loginRateLimiter = loginRateLimiter;
        this.auditPort = auditPort;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.clientAddressResolver = clientAddressResolver;
        this.clock = applicationClock;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> login(
            @Valid @RequestBody IdentityLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String sourceIp = clientAddressResolver.resolve(servletRequest);
        String userAgent = servletRequest.getHeader(USER_AGENT_HEADER);

        if (!loginRateLimiter.allowLogin(
                sourceIp,
                normalizedEmail
        )) {
            auditPort.record(
                    null,
                    IdentityAuditEventType.USER_LOGIN_RATE_LIMITED,
                    sourceIp,
                    userAgent
            );

            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new IdentityAuthenticationErrorResponse(
                            LOGIN_THROTTLED_MESSAGE
                    ));
        }

        String normalizedPassword = Normalizer.normalize(
                request.password(),
                Normalizer.Form.NFC
        );

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated(
                                    normalizedEmail,
                                    normalizedPassword
                            )
                    );

            if (!(authentication.getPrincipal()
                    instanceof IdentityAuthenticatedUser principal)) {
                throw new AuthenticationServiceException(
                        "Unexpected authentication principal type."
                );
            }

            /*
             * CSRF token acquisition normally created this session.
             * Create one defensively before rotating the session ID.
             */
            servletRequest.getSession(true);

            /*
             * Explicitly invoke session fixation protection because
             * authentication is performed by this JSON controller.
             */
            sessionAuthenticationStrategy.onAuthentication(
                    authentication,
                    servletRequest,
                    servletResponse
            );

            HttpSession session = servletRequest.getSession(false);

            if (session == null) {
                throw new AuthenticationServiceException(
                        "Authentication session could not be created."
                );
            }

            session.setAttribute(
                    SecuritySessionAttributes.AUTHENTICATED_AT,
                    clock.millis()
            );

            /*
             * Rotate the CSRF token after authentication.
             * The frontend must retrieve a fresh token after login.
             */
            csrfTokenRepository.saveToken(
                    null,
                    servletRequest,
                    servletResponse
            );

            auditPort.record(
                    principal.getUserAccountId(),
                    IdentityAuditEventType.USER_LOGIN_SUCCEEDED,
                    sourceIp,
                    userAgent
            );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            /*
             * Persist the authenticated security context in the
             * server-managed HTTP session.
             */
            securityContextRepository.saveContext(
                    context,
                    servletRequest,
                    servletResponse
            );

            return ResponseEntity.ok(
                    new IdentitySessionResponse(
                            toUserResponse(principal)
                    )
            );

        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            /*
             * Do not reveal whether the account is unknown, has an
             * incorrect password, is unverified, suspended or disabled.
             */
            auditPort.record(
                    null,
                    IdentityAuditEventType.USER_LOGIN_FAILED,
                    sourceIp,
                    userAgent
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new IdentityAuthenticationErrorResponse(
                            INVALID_CREDENTIALS_MESSAGE
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<IdentityUserResponse> currentIdentity(
            @AuthenticationPrincipal IdentityAuthenticatedUser principal
    ) {
        return ResponseEntity.ok(toUserResponse(principal));
    }

    @DeleteMapping("/sessions/current")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal IdentityAuthenticatedUser principal,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        auditPort.record(
                principal.getUserAccountId(),
                IdentityAuditEventType.USER_LOGOUT_SUCCEEDED,
                clientAddressResolver.resolve(servletRequest),
                servletRequest.getHeader(USER_AGENT_HEADER)
        );

        csrfTokenRepository.saveToken(
                null,
                servletRequest,
                servletResponse
        );

        SecurityContext emptyContext =
                SecurityContextHolder.createEmptyContext();

        securityContextRepository.saveContext(
                emptyContext,
                servletRequest,
                servletResponse
        );

        SecurityContextHolder.clearContext();

        HttpSession session = servletRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }

    private IdentityUserResponse toUserResponse(
            IdentityAuthenticatedUser principal
    ) {
        return new IdentityUserResponse(
                principal.getUserAccountId(),
                principal.getDisplayName(),
                principal.getEmail(),
                principal.isEmailVerified()
        );
    }
}