package com.tourswitch.domain.auth.response;

import com.tourswitch.global.security.jwt.TokenPair;

public record RefreshResponseDTO(
    String accessToken,
    long expiresIn
) {

    public static RefreshResponseDTO from(
        TokenPair tokenPair
    ) {
        return new RefreshResponseDTO(
            tokenPair.accessToken(),
            tokenPair.accessTokenExpiresIn()
        );
    }
}