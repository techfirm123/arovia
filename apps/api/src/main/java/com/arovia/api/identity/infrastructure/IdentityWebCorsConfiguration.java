
package com.arovia.api.identity.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IdentityWebCorsConfiguration implements WebMvcConfigurer {

    private final String allowedOrigin;

    public IdentityWebCorsConfiguration(
            @Value("${arovia.web.allowed-origin:http://localhost:3000}")
            String allowedOrigin
    ) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/identity/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("Content-Type")
                .maxAge(3600);
    }
}
