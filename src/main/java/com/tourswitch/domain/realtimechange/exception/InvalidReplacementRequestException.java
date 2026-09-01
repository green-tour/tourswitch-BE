package com.tourswitch.domain.realtimechange.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class InvalidReplacementRequestException extends BusinessException {

    public InvalidReplacementRequestException(String message) {
        super(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR, message);
    }
}
