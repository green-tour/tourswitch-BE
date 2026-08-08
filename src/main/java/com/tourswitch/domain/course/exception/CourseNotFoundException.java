package com.tourswitch.domain.course.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class CourseNotFoundException extends BusinessException {

    public CourseNotFoundException(String message) {
        super(CustomResponseCode.NODATA_ERROR, message);
    }
}
