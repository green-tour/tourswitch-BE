package com.tourswitch.global.security.oauth2;

import com.tourswitch.global.config.security.FrontendProperties;
import com.tourswitch.global.error.CustomResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler
    implements AuthenticationFailureHandler {

    private static final String WITHDRAWN_MEMBER_ERROR =
        "withdrawn_member";

    private final FrontendProperties frontendProperties;

    @Override
    public void onAuthenticationFailure(
        @NonNull HttpServletRequest request,
        HttpServletResponse response,
        @NonNull AuthenticationException exception
    ) throws IOException, ServletException {

        String responseCode =
            resolveResponseCode(exception);

        String redirectUri =
            UriComponentsBuilder
                .fromUriString(frontendProperties.callbackUri())
                .queryParam("code", responseCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUri);
    }

    private String resolveResponseCode(
        AuthenticationException exception
    ) {
        if (
            exception
                instanceof OAuth2AuthenticationException oauthException
        ) {
            String errorCode =
                oauthException
                    .getError()
                    .getErrorCode();

            if (WITHDRAWN_MEMBER_ERROR.equals(errorCode)) {
                return CustomResponseCode
                    .WITHDRAWN_USER
                    .getCode();
            }
        }

        return CustomResponseCode
            .KAKAO_OAUTH_ERROR
            .getCode();
    }
}