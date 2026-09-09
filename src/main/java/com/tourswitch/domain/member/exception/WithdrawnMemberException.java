package com.tourswitch.domain.member.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class WithdrawnMemberException extends BusinessException {

    public WithdrawnMemberException() {
        super(
            CustomResponseCode.WITHDRAWN_USER,
            "탈퇴한 회원은 서비스를 이용할 수 없습니다."
        );
    }
}