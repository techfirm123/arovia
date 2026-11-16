package com.arovia.api.identity.web;

import java.util.UUID;

public record IdentityUserResponse(
        UUID id,
        String displayName,
        String email,
        boolean emailVerified
) {
}