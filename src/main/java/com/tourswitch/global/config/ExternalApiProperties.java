package com.tourswitch.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour-api")
public record ExternalApiProperties(String secretKey, String mobileOs, String mobileApp) {
}
