package com.arovia.api.identity.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@Configuration
@EnableWebSecurity
public class IdentityWebSecurityConfiguration {

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);

        return new ProviderManager(List.of(provider));
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository =
                new HttpSessionCsrfTokenRepository();

        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setParameterName("_csrf");

        return repository;
    }

    /*
     * JSON login is performed by our controller rather than by
     * UsernamePasswordAuthenticationFilter. Therefore, the controller
     * must explicitly invoke the session authentication strategy.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${arovia.identity.session.secure-cookie:false}")
            boolean secureCookie
    ) {
        DefaultCookieSerializer serializer =
                new DefaultCookieSerializer();

        serializer.setCookieName("AROVIA_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secureCookie);
        serializer.setSameSite("Lax");

        /*
         * No domain is configured. The session cookie remains host-only.
         */
        return serializer;
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            Clock applicationClock,
            @Value("${arovia.identity.session.absolute-timeout:PT12H}")
            Duration absoluteSessionTimeout
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                /*
                 * CSRF is enabled for browser state-changing requests,
                 * including registration, email verification and login.
                 */
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
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/api/v1/system/health"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/identity/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/identity/registrations",
                                "/api/v1/identity/email-verification/confirm",
                                "/api/v1/identity/email-verification/resend",
                                "/api/v1/identity/sessions",
                                "/api/v1/identity/password-resets",
                                "/api/v1/identity/password-resets/confirm"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/identity/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/identity/sessions/current"
                        ).authenticated()

                        /*
                         * New API routes must be explicitly authorized.
                         */
                        .anyRequest().denyAll()
                );

        /*
         * This filter enforces the maximum authenticated session age.
         * It is intentionally not registered as a second servlet filter.
         */
        http.addFilterAfter(
                new AbsoluteSessionLifetimeFilter(
                        applicationClock,
                        absoluteSessionTimeout
                ),
                SecurityContextHolderFilter.class
        );

        return http.build();
    }
}