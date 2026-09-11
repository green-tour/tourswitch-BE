package com.tourswitch.domain.room.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class InviteParticipationNotAllowedException extends BusinessException {
    public InviteParticipationNotAllowedException() {
        super(CustomResponseCode.SESSION_STATE_CONFLICT, "종료된 여행방에는 참여할 수 없습니다.");
    }
}
