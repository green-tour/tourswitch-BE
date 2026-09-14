package com.tourswitch.global.client.seoul;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seoul-open-api")
public record SeoulOpenApiProperties(
        String baseUrl,
        String serviceName,
        String serviceKey,
        int connectTimeoutMs,
        int readTimeoutMs,
        boolean collectionEnabled,
        long collectionDelayMs
) {
}
