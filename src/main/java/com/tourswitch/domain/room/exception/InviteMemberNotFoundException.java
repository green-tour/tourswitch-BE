package com.tourswitch.domain.room.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class InviteMemberNotFoundException extends BusinessException {
    public InviteMemberNotFoundException() {
        super(CustomResponseCode.NODATA_ERROR, "회원을 찾을 수 없습니다.");
    }
}
