package com.tourswitch.domain.auth.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequestDTO(
    @NotBlank(message = "인가 코드는 필수입니다.")
    String oauthCode,

    @NotBlank(message = "Redirect URI는 필수입니다.")
    String redirectUri
) {
}
