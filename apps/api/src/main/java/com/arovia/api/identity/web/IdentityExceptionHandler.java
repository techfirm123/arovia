package com.arovia.api.identity.web;

import java.net.URI;

import com.arovia.api.identity.application.InvalidEmailVerificationTokenException;
import com.arovia.api.identity.application.PasswordPolicyException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidEmailVerificationToken(
            InvalidEmailVerificationTokenException exception,
            HttpServletRequest request
    ) {
        return badRequest(exception.getMessage(), request);
    }

    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<ProblemDetail> handlePasswordPolicyViolation(
            PasswordPolicyException exception,
            HttpServletRequest request
    ) {
        return badRequest(exception.getMessage(), request);
    }

    private ResponseEntity<ProblemDetail> badRequest(
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail
        );

        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }
}