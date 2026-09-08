package com.tourswitch.domain.auth.controller;

import com.tourswitch.domain.auth.response.RefreshResponseDTO;
import com.tourswitch.domain.auth.service.RefreshTokenService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

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