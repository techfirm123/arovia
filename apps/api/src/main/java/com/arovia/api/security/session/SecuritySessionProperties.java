package com.arovia.api.security.session;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "arovia.security.session")
public class SecuritySessionProperties {

    @NotNull
    private Duration absoluteTimeout = Duration.ofHours(12);

    private boolean secureCookie;

    public Duration getAbsoluteTimeout() {
        return absoluteTimeout;
    }

    public void setAbsoluteTimeout(Duration absoluteTimeout) {
        this.absoluteTimeout = absoluteTimeout;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }

    public void setSecureCookie(boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    @AssertTrue(
            message = "arovia.security.session.absolute-timeout must be positive."
    )
    public boolean isAbsoluteTimeoutPositive() {
        return absoluteTimeout != null
                && !absoluteTimeout.isZero()
                && !absoluteTimeout.isNegative();
    }
}