package com.arovia.api.notifications.infrastructure;

public class OutboxEncryptionKeyUnavailableException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OutboxEncryptionKeyUnavailableException(String keyVersion) {
        super(
                "Outbox encryption key version is not configured: "
                        + keyVersion
        );
    }
}