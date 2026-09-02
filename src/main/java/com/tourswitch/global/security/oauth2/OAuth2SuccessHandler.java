package com.tourswitch.global.security.oauth2;

import com.tourswitch.global.config.security.FrontendProperties;
import com.tourswitch.global.error.CustomResponseCode;
import com.tourswitch.global.security.cookie.RefreshTokenCookieManager;
import com.tourswitch.global.security.jwt.JwtProvider;
import com.tourswitch.global.security.jwt.TokenPair;
import com.tourswitch.domain.auth.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
    implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final FrontendProperties frontendProperties;

    @Override
    public void onAuthenticationSuccess(
        @NonNull HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        OAuth2MemberPrincipal principal =
            (OAuth2MemberPrincipal) authentication.getPrincipal();

        assert principal != null;
        Long memberId = principal.memberId();

        TokenPair tokenPair =
            jwtProvider.createTokenPair(memberId);

        refreshTokenService.saveRefreshToken(
            memberId,
            tokenPair
        );

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshTokenCookieManager
                .createRefreshTokenCookie(
                    tokenPair.refreshToken()
                )
                .toString()
        );

        String redirectUri =
            UriComponentsBuilder
                .fromUriString(
                    frontendProperties.callbackUri()
                )
                .queryParam(
                    "code",
                    CustomResponseCode.NORMAL_CODE.getCode()
                )
                .queryParam(
                    "newMember",
                    principal.newUser()
                )
                .build()
                .toUriString();

        response.sendRedirect(redirectUri);
    }
}