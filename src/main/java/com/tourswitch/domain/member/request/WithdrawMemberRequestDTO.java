package com.tourswitch.domain.member.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WithdrawMemberRequestDTO(

    @NotBlank(message = "탈퇴 확인값은 필수입니다.")
    @Pattern(
        regexp = "^WITHDRAW$",
        message = "탈퇴 확인값이 올바르지 않습니다."
    )
    String confirmation

) {
}