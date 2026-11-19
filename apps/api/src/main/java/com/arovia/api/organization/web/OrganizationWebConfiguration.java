package com.arovia.api.organization.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class OrganizationWebConfiguration
        implements WebMvcConfigurer {

    private final OrganizationContextInterceptor contextInterceptor;

    public OrganizationWebConfiguration(
            OrganizationContextInterceptor contextInterceptor
    ) {
        this.contextInterceptor = contextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contextInterceptor);
    }
}