
package com.arovia.api.identity.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PasswordPolicyException extends ResponseStatusException {

    public PasswordPolicyException() {
        super(
                HttpStatus.BAD_REQUEST,
                "Password does not meet the account security requirements."
        );
    }
}
