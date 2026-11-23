
package com.arovia.api.security.configuration;

import java.time.Clock;

import com.arovia.api.security.session.AbsoluteSessionLifetimeFilter;
import com.arovia.api.security.session.SecuritySessionProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityWebConfiguration {

    private static final String SESSION_COOKIE_NAME = "AROVIA_SESSION";

    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    private static final String CSRF_PARAMETER_NAME = "_csrf";

    private static final String ACTUATOR_LIVENESS_ENDPOINT =
            "/actuator/health/liveness";

    private static final String ACTUATOR_READINESS_ENDPOINT =
            "/actuator/health/readiness";

    private static final String IDENTITY_CSRF_ENDPOINT =
            "/api/v1/identity/csrf";

    private static final String IDENTITY_REGISTRATION_ENDPOINT =
            "/api/v1/identity/registrations";

    private static final String IDENTITY_EMAIL_VERIFICATION_CONFIRM_ENDPOINT =
            "/api/v1/identity/email-verification/confirm";

    private static final String IDENTITY_EMAIL_VERIFICATION_RESEND_ENDPOINT =
            "/api/v1/identity/email-verification/resend";

    private static final String IDENTITY_SESSION_ENDPOINT =
            "/api/v1/identity/sessions";

    private static final String IDENTITY_PASSWORD_RESET_ENDPOINT =
            "/api/v1/identity/password-resets";

    private static final String IDENTITY_PASSWORD_RESET_CONFIRM_ENDPOINT =
            "/api/v1/identity/password-resets/confirm";

    private static final String IDENTITY_CURRENT_USER_ENDPOINT =
            "/api/v1/identity/me";

    private static final String IDENTITY_CURRENT_SESSION_ENDPOINT =
            "/api/v1/identity/sessions/current";

    private static final String ORGANIZATION_COLLECTION_ENDPOINT =
            "/api/v1/organizations";

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository =
                new HttpSessionCsrfTokenRepository();

        repository.setHeaderName(CSRF_HEADER_NAME);
        repository.setParameterName(CSRF_PARAMETER_NAME);

        return repository;
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public CookieSerializer cookieSerializer(
            SecuritySessionProperties sessionProperties
    ) {
        DefaultCookieSerializer serializer =
                new DefaultCookieSerializer();

        serializer.setCookieName(SESSION_COOKIE_NAME);
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(
                sessionProperties.isSecureCookie()
        );
        serializer.setSameSite("Lax");

        return serializer;
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            Clock applicationClock,
            SecuritySessionProperties sessionProperties
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                )

                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )

                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                HttpMethod.GET,
                                ACTUATOR_LIVENESS_ENDPOINT,
                                ACTUATOR_READINESS_ENDPOINT
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                IDENTITY_CSRF_ENDPOINT
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                IDENTITY_REGISTRATION_ENDPOINT,
                                IDENTITY_EMAIL_VERIFICATION_CONFIRM_ENDPOINT,
                                IDENTITY_EMAIL_VERIFICATION_RESEND_ENDPOINT,
                                IDENTITY_SESSION_ENDPOINT,
                                IDENTITY_PASSWORD_RESET_ENDPOINT,
                                IDENTITY_PASSWORD_RESET_CONFIRM_ENDPOINT
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                IDENTITY_CURRENT_USER_ENDPOINT
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                IDENTITY_CURRENT_SESSION_ENDPOINT
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                ORGANIZATION_COLLECTION_ENDPOINT
                        ).authenticated()
                        .requestMatchers("/api/v1/organizations/**").authenticated()
                        .requestMatchers("/api/v1/customers/**").authenticated()
                        .anyRequest().denyAll()
                );

        http.addFilterAfter(
                new AbsoluteSessionLifetimeFilter(
                        applicationClock,
                        sessionProperties.getAbsoluteTimeout()
                ),
                SecurityContextHolderFilter.class
        );

        return http.build();
    }
}
