package com.tourswitch.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seoul-open-api")
public record SeoulApiProperties(String baseUrl, String secretKey) {

    public String resolvedBaseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "http://openapi.seoul.go.kr:8088" : baseUrl.strip();
    }
}
