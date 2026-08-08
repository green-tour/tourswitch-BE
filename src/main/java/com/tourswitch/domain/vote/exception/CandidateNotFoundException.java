package com.tourswitch.domain.vote.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class CandidateNotFoundException extends BusinessException {

    public CandidateNotFoundException(String message) {
        super(CustomResponseCode.NODATA_ERROR, message);
    }
}
