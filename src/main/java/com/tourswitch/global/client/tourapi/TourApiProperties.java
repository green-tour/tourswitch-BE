package com.tourswitch.global.client.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(
        String baseUrl,
        String serviceKey,
        String accessibilityServiceKey,
        String mobileOs,
        String mobileApp,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxAttempts,
        long retryDelayMs,
        boolean accessibilityEnabled,
        int detailConcurrency
) {

    public int resolvedConnectTimeoutMs() {
        return connectTimeoutMs > 0 ? connectTimeoutMs : 3_000;
    }

    public int resolvedReadTimeoutMs() {
        return readTimeoutMs > 0 ? readTimeoutMs : 20_000;
    }

    public int resolvedMaxAttempts() {
        return maxAttempts > 0 ? maxAttempts : 3;
    }

    public long resolvedRetryDelayMs() {
        return retryDelayMs > 0 ? retryDelayMs : 500L;
    }

    public int resolvedDetailConcurrency() {
        return detailConcurrency > 0 ? detailConcurrency : 4;
    }

    public String resolvedAccessibilityServiceKey() {
        return accessibilityServiceKey == null || accessibilityServiceKey.isBlank()
                ? serviceKey
                : accessibilityServiceKey;
    }
}
