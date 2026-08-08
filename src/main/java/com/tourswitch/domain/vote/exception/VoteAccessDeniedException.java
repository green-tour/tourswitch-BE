package com.tourswitch.domain.vote.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class VoteAccessDeniedException extends BusinessException {

    public VoteAccessDeniedException(String message) {
        super(CustomResponseCode.SERVICE_ACCESS_DENIED_ERROR, message);
    }
}
