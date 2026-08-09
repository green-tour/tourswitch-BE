package com.tourswitch.domain.course.exception;

import com.tourswitch.global.error.BusinessException;
import com.tourswitch.global.error.CustomResponseCode;

public class CourseAccessDeniedException extends BusinessException {

    public CourseAccessDeniedException(String message) {
        super(CustomResponseCode.SERVICE_ACCESS_DENIED_ERROR, message);
    }
}
