package com.tourswitch.domain.member.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class MemberNotFoundException extends BusinessException {

    public MemberNotFoundException() {
        super(
            CustomResponseCode.NODATA_ERROR,
            "회원을 찾을 수 없습니다."
        );
    }
}