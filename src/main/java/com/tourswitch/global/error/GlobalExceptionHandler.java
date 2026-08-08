package com.tourswitch.global.error;

import com.tourswitch.global.response.GlobalRes;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalRes<Void>> handleBusinessException(BusinessException e) {
        CustomResponseCode code = e.getCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", code.getCode(), e.getMessage(), e);
        } else {
            log.warn("[{}] {}", code.getCode(), e.getMessage());
        }
        return ResponseEntity.status(code.getHttpStatus()).body(GlobalRes.fail(code, e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .findFirst()
                .orElse("잘못된 요청 파라미터입니다.");
        log.warn("[{}] {}", CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR.getCode(), message);
        return ResponseEntity.status(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR.getHttpStatus())
                .body(GlobalRes.fail(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR, message, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalRes<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("[{}] {}", CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR.getCode(), e.getMessage());
        return ResponseEntity.status(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR.getHttpStatus())
                .body(GlobalRes.fail(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR, e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalRes<Void>> handleException(Exception e) {
        log.error("[{}] {}", CustomResponseCode.UNKNOWN_ERROR.getCode(), e.getMessage(), e);
        return ResponseEntity.status(CustomResponseCode.UNKNOWN_ERROR.getHttpStatus())
                .body(GlobalRes.fail(CustomResponseCode.UNKNOWN_ERROR, "알 수 없는 오류가 발생했습니다.", null));
    }
}
