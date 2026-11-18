package com.arovia.api.security.client;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "arovia.security.client-address")
public class ClientAddressProperties {

    @NotNull
    private List<String> trustedProxies = new ArrayList<>();

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies == null
                ? new ArrayList<>()
                : new ArrayList<>(trustedProxies);
    }
}