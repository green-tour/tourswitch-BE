package com.tourswitch.domain.realtimechange.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class RealtimeChangeConflictException extends BusinessException {

    public RealtimeChangeConflictException(String message) {
        super(CustomResponseCode.SESSION_STATE_CONFLICT, message);
    }
}
