package com.tourswitch.global.security.jwt;

import java.time.LocalDateTime;

public record TokenPair(
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn,
    LocalDateTime refreshTokenExpiresAt
) {
}