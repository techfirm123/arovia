package com.arovia.api.identity.web;

public record CsrfTokenResponse(
        String headerName,
        String token
) {
}