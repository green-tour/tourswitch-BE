package com.tourswitch.global.security.handler;

import com.tourswitch.global.error.CustomResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler
    implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        securityErrorResponseWriter.write(
            response,
            CustomResponseCode.SERVICE_ACCESS_DENIED_ERROR,
            "접근 권한이 없습니다."
        );
    }
}