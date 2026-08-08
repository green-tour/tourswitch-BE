package com.tourswitch.global.error;

import org.springframework.http.HttpStatus;

public enum CustomResponseCode {

    NORMAL_CODE("00", HttpStatus.OK),
    APPLICATION_ERROR("01", HttpStatus.INTERNAL_SERVER_ERROR),
    DB_ERROR("02", HttpStatus.INTERNAL_SERVER_ERROR),
    NODATA_ERROR("03", HttpStatus.NOT_FOUND),
    HTTP_ERROR("04", HttpStatus.BAD_GATEWAY),
    SERVICETIMEOUT_ERROR("05", HttpStatus.GATEWAY_TIMEOUT),
    UNAUTHENTICATED_ERROR("06", HttpStatus.UNAUTHORIZED),
    TOKEN_ERROR("07", HttpStatus.UNAUTHORIZED),
    DUPLICATE_DATA_ERROR("08", HttpStatus.CONFLICT),
    FILE_PROCESS_ERROR("09", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_PARAMETER_ERROR("10", HttpStatus.BAD_REQUEST),
    NO_MANDATORY_REQUEST_PARAMETERS_ERROR("11", HttpStatus.BAD_REQUEST),
    NO_OPENAPI_SERVICE_ERROR("12", HttpStatus.BAD_GATEWAY),
    SERVICE_ACCESS_DENIED_ERROR("20", HttpStatus.FORBIDDEN),
    TEMPORARILY_DISABLE_THE_SERVICEKEY_ERROR("21", HttpStatus.SERVICE_UNAVAILABLE),
    LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR("22", HttpStatus.TOO_MANY_REQUESTS),
    SERVICE_KEY_IS_NOT_REGISTERED_ERROR("30", HttpStatus.BAD_GATEWAY),
    DEADLINE_HAS_EXPIRED_ERROR("31", HttpStatus.BAD_GATEWAY),
    UNREGISTERED_IP_ERROR("32", HttpStatus.BAD_GATEWAY),
    UNSIGNED_CALL_ERROR("33", HttpStatus.BAD_GATEWAY),
    UNKNOWN_ERROR("99", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus httpStatus;

    CustomResponseCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
