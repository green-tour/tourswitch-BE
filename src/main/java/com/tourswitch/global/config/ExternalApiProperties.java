package com.tourswitch.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour-api")
public record ExternalApiProperties(
        String secretKey,
        String mobileOs,
        String mobileApp,
        boolean accessibilityEnabled,
        int detailConcurrency
) {

    public int resolvedDetailConcurrency() {
        return detailConcurrency > 0 ? detailConcurrency : 4;
    }
}
