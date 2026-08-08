package com.tourswitch.domain.vote.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class VoteSessionNotActiveException extends BusinessException {

    public VoteSessionNotActiveException(String message) {
        super(CustomResponseCode.SESSION_STATE_CONFLICT, message);
    }
}
