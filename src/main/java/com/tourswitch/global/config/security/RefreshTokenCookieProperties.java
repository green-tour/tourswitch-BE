package com.tourswitch.global.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "refresh-token.cookie")
public record RefreshTokenCookieProperties(
    String name,
    boolean secure,
    String sameSite,
    String path
) {
}