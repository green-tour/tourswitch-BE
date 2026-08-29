package com.tourswitch.domain.auth.controller;

import com.tourswitch.domain.auth.request.KakaoLoginRequestDTO;
import com.tourswitch.domain.auth.response.LoginResponseDTO;
import com.tourswitch.domain.auth.response.RefreshResponseDTO;
import com.tourswitch.domain.auth.service.AuthLoginResult;
import com.tourswitch.domain.auth.service.AuthService;
import com.tourswitch.domain.auth.service.RefreshTokenService;
import com.tourswitch.global.response.GlobalRes;
import com.tourswitch.global.security.cookie.RefreshTokenCookieManager;
import com.tourswitch.global.security.jwt.TokenPair;
import com.tourswitch.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * 카카오 로그인
     */
    @PostMapping("/login")
    public GlobalRes<LoginResponseDTO> login(
        @Valid @RequestBody KakaoLoginRequestDTO request,
        HttpServletResponse response
    ) {
        AuthLoginResult result =
            authService.login(request);

        ResponseCookie refreshTokenCookie =
            refreshTokenCookieManager
                .createRefreshTokenCookie(
                    result.tokenPair().refreshToken()
                );

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshTokenCookie.toString()
        );

        LoginResponseDTO loginResponse =
            LoginResponseDTO.from(result);

        return GlobalRes.success(loginResponse);
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