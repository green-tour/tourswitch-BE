package com.tourswitch.domain.room.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class InvalidTravelRoomRequestException extends BusinessException {
    public InvalidTravelRoomRequestException(String message) {
        super(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR, message);
    }
}
