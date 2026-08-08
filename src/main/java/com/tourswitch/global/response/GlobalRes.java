package com.tourswitch.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tourswitch.global.error.CustomResponseCode;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GlobalRes<T>(String code, String message, T data) {

    public static <T> GlobalRes<T> success(T data) {
        return new GlobalRes<>(CustomResponseCode.NORMAL_CODE.getCode(), "정상 처리되었습니다.", data);
    }

    public static GlobalRes<Void> success() {
        return new GlobalRes<>(CustomResponseCode.NORMAL_CODE.getCode(), "정상 처리되었습니다.", null);
    }

    public static <T> GlobalRes<T> fail(CustomResponseCode code, String message, T data) {
        return new GlobalRes<>(code.getCode(), message, data);
    }
}
