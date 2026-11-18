package com.arovia.api.configuration;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "arovia")
public class AroviaProperties {

    @Valid
    @NotNull
    private App app = new App();

    @Valid
    @NotNull
    private Mail mail = new Mail();

    @Valid
    @NotNull
    private Outbox outbox = new Outbox();

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    public static class App {

        @NotBlank
        private String webBaseUrl = "http://localhost:3000";

        public String getWebBaseUrl() {
            return webBaseUrl;
        }

        public void setWebBaseUrl(String webBaseUrl) {
            this.webBaseUrl = webBaseUrl;
        }
    }

    public static class Mail {

        @NotBlank
        private String from = "no-reply@arovia.local";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Outbox {

        @Positive
        private long pollIntervalMs = 5_000;

        @Positive
        private int batchSize = 10;

        @Valid
        @NotNull
        private Encryption encryption = new Encryption();

        @Valid
        @NotNull
        private Retry retry = new Retry();

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Encryption getEncryption() {
            return encryption;
        }

        public void setEncryption(Encryption encryption) {
            this.encryption = encryption;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public static class Encryption {

            private static final String KEY_VERSION_REGEX =
                    "[A-Za-z0-9][A-Za-z0-9._-]{0,79}";

            @NotBlank
            private String key = "";

            @NotBlank
            @Pattern(regexp = KEY_VERSION_REGEX)
            private String keyVersion = "local-v1";

            /*
             * Additional keys are decrypt-only.
             *
             * The active key is used for all new encryption.
             * Additional keys allow existing payloads, or payloads
             * encrypted during a staged key rotation, to be decrypted.
             */
            private Map<String, String> additionalKeys =
                    new LinkedHashMap<>();

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public String getKeyVersion() {
                return keyVersion;
            }

            public void setKeyVersion(String keyVersion) {
                this.keyVersion = keyVersion;
            }

            public Map<String, String> getAdditionalKeys() {
                return additionalKeys;
            }

            public void setAdditionalKeys(
                    Map<String, String> additionalKeys
            ) {
                this.additionalKeys = additionalKeys == null
                        ? null
                        : new LinkedHashMap<>(additionalKeys);
            }

            @AssertTrue(
                    message = "Additional outbox encryption keys must have "
                            + "valid, unique versions and nonblank key values."
            )
            public boolean isAdditionalKeyConfigurationValid() {
                if (additionalKeys == null) {
                    return false;
                }

                for (Map.Entry<String, String> entry
                        : additionalKeys.entrySet()) {

                    String version = entry.getKey();
                    String value = entry.getValue();

                    if (!isValidKeyVersion(version)) {
                        return false;
                    }

                    if (version.equals(keyVersion)) {
                        return false;
                    }

                    if (value == null || value.isBlank()) {
                        return false;
                    }
                }

                return true;
            }

            private boolean isValidKeyVersion(String version) {
                return version != null
                        && version.matches(KEY_VERSION_REGEX);
            }
        }

        public static class Retry {

            @Positive
            private int maxAttempts = 8;

            @Positive
            private long baseRetryDelaySeconds = 60;

            @Positive
            private long maxRetryDelaySeconds = 3_600;

            @PositiveOrZero
            private long maxJitterSeconds = 30;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public long getBaseRetryDelaySeconds() {
                return baseRetryDelaySeconds;
            }

            public void setBaseRetryDelaySeconds(
                    long baseRetryDelaySeconds
            ) {
                this.baseRetryDelaySeconds = baseRetryDelaySeconds;
            }

            public long getMaxRetryDelaySeconds() {
                return maxRetryDelaySeconds;
            }

            public void setMaxRetryDelaySeconds(long maxRetryDelaySeconds) {
                this.maxRetryDelaySeconds = maxRetryDelaySeconds;
            }

            public long getMaxJitterSeconds() {
                return maxJitterSeconds;
            }

            public void setMaxJitterSeconds(long maxJitterSeconds) {
                this.maxJitterSeconds = maxJitterSeconds;
            }

            @AssertTrue(
                    message = "Outbox base retry delay cannot exceed maximum retry delay."
            )
            public boolean isRetryDelayRangeValid() {
                return baseRetryDelaySeconds > 0
                        && maxRetryDelaySeconds > 0
                        && baseRetryDelaySeconds <= maxRetryDelaySeconds;
            }
        }
    }
}