package com.tourswitch.domain.member.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateNicknameRequestDTO(
    @NotBlank(message = "닉네임은 필수입니다.")
    String nickname
) {
}
