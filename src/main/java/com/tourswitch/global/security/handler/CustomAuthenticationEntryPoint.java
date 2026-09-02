package com.tourswitch.global.security.handler;

import com.tourswitch.global.error.CustomResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@NullMarked
public class CustomAuthenticationEntryPoint
    implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {

        securityErrorResponseWriter.write(
            response,
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            "인증이 필요합니다."
        );
    }
}