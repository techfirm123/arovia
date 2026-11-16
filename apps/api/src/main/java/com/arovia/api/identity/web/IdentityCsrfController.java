package com.arovia.api.identity.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityCsrfController {

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrfToken(
            CsrfToken csrfToken
    ) {
        /*
         * Accessing getToken() resolves the deferred token and
         * persists it in the server-side HTTP session.
         *
         * The returned token is the request-appropriate token
         * exposed by Spring Security's CSRF request handler.
         */
        CsrfTokenResponse response = new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getToken()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}