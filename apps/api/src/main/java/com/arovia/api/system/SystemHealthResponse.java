package com.arovia.api.system;

import java.time.Instant;

public record SystemHealthResponse(
        String status,
        String application,
        Instant timestamp
) {
}