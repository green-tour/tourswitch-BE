package com.tourswitch.domain.auth.controller;

import com.tourswitch.domain.auth.response.RefreshResponseDTO;
import com.tourswitch.domain.auth.service.RefreshTokenService;
import com.tourswitch.global.config.security.FrontendProperties;
import com.tourswitch.global.response.GlobalRes;
import com.tourswitch.global.security.cookie.RefreshTokenCookieManager;
import com.tourswitch.global.security.jwt.TokenPair;
import com.tourswitch.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private static final String KAKAO_AUTHORIZATION_URI =
        "/api/auth/oauth2/authorization/kakao";

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final FrontendProperties frontendProperties;

    /**
     * 카카오 로그인 시작
     * 카카오 콜백이 FE 도메인으로 돌아오므로 인가 요청도 FE 도메인(프록시)에서 시작해야
     * 인가 세션 쿠키가 콜백에 전달된다. 상대 경로로 두면 프록시 뒤의 BE 호스트로 리다이렉트된다.
     */
    @GetMapping("/login")
    public RedirectView login() {
        String frontendOrigin = frontendProperties.origin().replaceAll("/+$", "");
        return new RedirectView(frontendOrigin + KAKAO_AUTHORIZATION_URI);
    }

    /**
     * 기존 API 명세의 POST 로그인 요청 호환
     */
    @PostMapping("/login")
    public RedirectView loginByPost() {
        return login();
    }

    /**
     * Access Token 재발급
     */
    @PostMapping("/refresh")
    public GlobalRes<RefreshResponseDTO> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken =
            refreshTokenCookieManager
                .getRefreshToken(request);

        TokenPair newTokenPair =
            refreshTokenService.refresh(
                refreshToken
            );

        ResponseCookie newRefreshTokenCookie =
            refreshTokenCookieManager
                .createRefreshTokenCookie(
                    newTokenPair.refreshToken()
                );

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            newRefreshTokenCookie.toString()
        );

        RefreshResponseDTO refreshResponse =
            RefreshResponseDTO.from(
                newTokenPair
            );

        return GlobalRes.success(refreshResponse);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public GlobalRes<Void> logout(
        @AuthenticationPrincipal UserPrincipal principal,
        HttpServletResponse response
    ) {
        refreshTokenService.logout(
            principal.memberId()
        );

        ResponseCookie expiredRefreshTokenCookie =
            refreshTokenCookieManager
                .createExpiredRefreshTokenCookie();

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            expiredRefreshTokenCookie.toString()
        );

        return GlobalRes.success();
    }
}
