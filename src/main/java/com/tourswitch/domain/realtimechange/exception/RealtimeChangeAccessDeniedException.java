package com.tourswitch.domain.realtimechange.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class RealtimeChangeAccessDeniedException extends BusinessException {

    public RealtimeChangeAccessDeniedException(String message) {
        super(CustomResponseCode.SERVICE_ACCESS_DENIED_ERROR, message);
    }
}
