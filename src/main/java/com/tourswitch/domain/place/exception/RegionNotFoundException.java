package com.tourswitch.domain.place.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class RegionNotFoundException extends BusinessException {
    public RegionNotFoundException() {
        super(CustomResponseCode.NODATA_ERROR, "존재하지 않는 지역입니다.");
    }
}
