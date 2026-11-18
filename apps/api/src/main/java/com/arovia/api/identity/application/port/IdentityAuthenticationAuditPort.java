package com.arovia.api.identity.application.port;

import java.util.UUID;

import com.arovia.api.identity.domain.IdentityAuditEventType;

public interface IdentityAuthenticationAuditPort {

    void record(
            UUID userAccountId,
            IdentityAuditEventType eventType,
            String sourceIp,
            String userAgent
    );
}