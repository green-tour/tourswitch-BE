package com.tourswitch.global.client.tourapi;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class TourApiClientException extends BusinessException {

    public TourApiClientException(CustomResponseCode code, String message) {
        super(code, message);
    }
}
