
package com.arovia.api.identity.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidEmailVerificationTokenException
        extends ResponseStatusException {

    public InvalidEmailVerificationTokenException() {
        super(
                HttpStatus.BAD_REQUEST,
                "The email verification link is invalid or has expired."
        );
    }
}
