package com.tourswitch.global.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
    String clientId,
    String clientSecret,
    String tokenUri,
    String userInfoUri,
    Duration connectTimeout,
    Duration readTimeout
) {
}
