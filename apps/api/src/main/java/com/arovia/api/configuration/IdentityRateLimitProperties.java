package com.arovia.api.identity.configuration;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "arovia.rate-limit")
public class IdentityRateLimitProperties {

    @NotBlank
    private String hmacKey = "";

    @Positive
    private long cleanupIntervalMs = 900_000;

    @Valid
    @NotNull
    private RateLimitPolicy registration = new RateLimitPolicy(
            Duration.ofHours(1),
            10,
            3
    );

    @Valid
    @NotNull
    private RateLimitPolicy resend = new RateLimitPolicy(
            Duration.ofHours(1),
            20,
            5
    );

    @Valid
    @NotNull
    private RateLimitPolicy login = new RateLimitPolicy(
            Duration.ofMinutes(15),
            60,
            10
    );

    @Valid
    @NotNull
    private PasswordResetRateLimitPolicy passwordReset =
            new PasswordResetRateLimitPolicy(
                    Duration.ofHours(1),
                    20,
                    5,
                    10
            );

    public String getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public RateLimitPolicy getRegistration() {
        return registration;
    }

    public void setRegistration(RateLimitPolicy registration) {
        this.registration = registration;
    }

    public RateLimitPolicy getResend() {
        return resend;
    }

    public void setResend(RateLimitPolicy resend) {
        this.resend = resend;
    }

    public RateLimitPolicy getLogin() {
        return login;
    }

    public void setLogin(RateLimitPolicy login) {
        this.login = login;
    }

    public PasswordResetRateLimitPolicy getPasswordReset() {
        return passwordReset;
    }

    public void setPasswordReset(
            PasswordResetRateLimitPolicy passwordReset
    ) {
        this.passwordReset = passwordReset;
    }

    public static class RateLimitPolicy {

        @NotNull
        private Duration window;

        @Positive
        private int ipLimit;

        @Positive
        private int emailLimit;

        public RateLimitPolicy() {
        }

        public RateLimitPolicy(
                Duration window,
                int ipLimit,
                int emailLimit
        ) {
            this.window = window;
            this.ipLimit = ipLimit;
            this.emailLimit = emailLimit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getIpLimit() {
            return ipLimit;
        }

        public void setIpLimit(int ipLimit) {
            this.ipLimit = ipLimit;
        }

        public int getEmailLimit() {
            return emailLimit;
        }

        public void setEmailLimit(int emailLimit) {
            this.emailLimit = emailLimit;
        }

        @AssertTrue(message = "Rate-limit window must be positive.")
        public boolean isWindowPositive() {
            return window != null
                    && !window.isZero()
                    && !window.isNegative()
                    && window.toSeconds() >= 1;
        }
    }

    public static class PasswordResetRateLimitPolicy
            extends RateLimitPolicy {

        @Positive
        private int confirmationLimit;

        public PasswordResetRateLimitPolicy() {
        }

        public PasswordResetRateLimitPolicy(
                Duration window,
                int ipLimit,
                int emailLimit,
                int confirmationLimit
        ) {
            super(window, ipLimit, emailLimit);
            this.confirmationLimit = confirmationLimit;
        }

        public int getConfirmationLimit() {
            return confirmationLimit;
        }

        public void setConfirmationLimit(int confirmationLimit) {
            this.confirmationLimit = confirmationLimit;
        }
    }
}