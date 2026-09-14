package com.tourswitch.global.security.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class TokenException extends BusinessException {

    public TokenException() {
        super(
            CustomResponseCode.TOKEN_ERROR,
            "유효하지 않은 토큰입니다."
        );
    }
}