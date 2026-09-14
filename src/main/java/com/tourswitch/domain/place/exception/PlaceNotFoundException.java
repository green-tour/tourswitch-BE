package com.tourswitch.domain.place.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class PlaceNotFoundException extends BusinessException {
    public PlaceNotFoundException() {
        super(CustomResponseCode.NODATA_ERROR, "존재하지 않는 관광지입니다.");
    }
}
