package com.tourswitch.domain.auth.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class KakaoOAuthException extends BusinessException {

    public KakaoOAuthException() {
        super(
            CustomResponseCode.KAKAO_OAUTH_ERROR,
            "카카오 로그인 처리에 실패했습니다."
        );
    }
}