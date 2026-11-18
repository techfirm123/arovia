package com.arovia.api.configuration;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "arovia.identity")
public class IdentityWorkflowProperties {

    @NotNull
    private Duration emailVerificationTtl = Duration.ofHours(24);

    @NotNull
    private Duration emailVerificationResendCooldown =
            Duration.ofMinutes(1);

    @NotNull
    private Duration passwordResetTtl = Duration.ofMinutes(30);

    public Duration getEmailVerificationTtl() {
        return emailVerificationTtl;
    }

    public void setEmailVerificationTtl(Duration emailVerificationTtl) {
        this.emailVerificationTtl = emailVerificationTtl;
    }

    public Duration getEmailVerificationResendCooldown() {
        return emailVerificationResendCooldown;
    }

    public void setEmailVerificationResendCooldown(
            Duration emailVerificationResendCooldown
    ) {
        this.emailVerificationResendCooldown =
                emailVerificationResendCooldown;
    }

    public Duration getPasswordResetTtl() {
        return passwordResetTtl;
    }

    public void setPasswordResetTtl(Duration passwordResetTtl) {
        this.passwordResetTtl = passwordResetTtl;
    }

    @AssertTrue(
            message = "Identity workflow durations must all be positive."
    )
    public boolean isWorkflowDurationsValid() {
        return isPositive(emailVerificationTtl)
                && isPositive(emailVerificationResendCooldown)
                && isPositive(passwordResetTtl);
    }

    private boolean isPositive(Duration duration) {
        return duration != null
                && !duration.isZero()
                && !duration.isNegative();
    }
}