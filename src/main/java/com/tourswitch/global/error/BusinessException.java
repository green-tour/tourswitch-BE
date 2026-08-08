package com.tourswitch.global.error;

public abstract class BusinessException extends RuntimeException {

    private final CustomResponseCode code;

    protected BusinessException(CustomResponseCode code, String message) {
        super(message);
        this.code = code;
    }

    public CustomResponseCode getCode() {
        return code;
    }
}
