package com.tourswitch.domain.realtimechange.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class RealtimeChangeNotFoundException extends BusinessException {

    public RealtimeChangeNotFoundException(String message) {
        super(CustomResponseCode.NODATA_ERROR, message);
    }
}
