package com.tourswitch.global.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    long accessTokenExpirationSeconds,
    long refreshTokenExpirationSeconds
) {
}