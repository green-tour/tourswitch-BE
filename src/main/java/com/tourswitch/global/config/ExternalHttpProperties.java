package com.tourswitch.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-http")
public record ExternalHttpProperties(
        int connectTimeoutMillis,
        int readTimeoutMillis,
        int maxAttempts,
        long retryDelayMillis
) {

    public int resolvedConnectTimeoutMillis() {
        return connectTimeoutMillis > 0 ? connectTimeoutMillis : 5_000;
    }

    public int resolvedReadTimeoutMillis() {
        return readTimeoutMillis > 0 ? readTimeoutMillis : 20_000;
    }

    public int resolvedMaxAttempts() {
        return maxAttempts > 0 ? maxAttempts : 3;
    }

    public long resolvedRetryDelayMillis() {
        return retryDelayMillis > 0 ? retryDelayMillis : 500L;
    }
}
