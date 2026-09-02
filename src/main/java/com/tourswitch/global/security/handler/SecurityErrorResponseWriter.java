package com.tourswitch.global.security.handler;

import com.tourswitch.global.error.CustomResponseCode;
import com.tourswitch.global.response.GlobalRes;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * Spring Security 영역의 오류를
     * 프로젝트 공통 응답 형식으로 반환
     */
    public void write(
        HttpServletResponse response,
        CustomResponseCode code,
        String message
    ) throws IOException {
        response.setStatus(
            code.getHttpStatus().value()
        );

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        GlobalRes<Void> body =
            GlobalRes.fail(
                code,
                message,
                null
            );

        objectMapper.writeValue(
            response.getOutputStream(),
            body
        );
    }
}