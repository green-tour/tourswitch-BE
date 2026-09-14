package com.tourswitch.domain.room.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class InviteNotFoundException extends BusinessException {
    public InviteNotFoundException() {
        super(CustomResponseCode.NODATA_ERROR, "유효하지 않은 초대 링크입니다.");
    }
}
