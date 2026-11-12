package com.arovia.api.system;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    @GetMapping("/health")
    public SystemHealthResponse health() {
        return new SystemHealthResponse(
                "UP",
                "arovia-api",
                Instant.now()
        );
    }
}