package com.tourswitch.global.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
    String callbackUri,
    String origin
) {
}