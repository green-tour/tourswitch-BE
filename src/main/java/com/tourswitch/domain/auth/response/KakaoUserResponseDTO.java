package com.tourswitch.domain.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponseDTO(
    Long id,

    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
        Profile profile
    ) {
    }

    public record Profile(
        String nickname
    ) {
    }
}