package com.tourswitch.global.security.cookie;

import com.tourswitch.global.config.security.JwtProperties;
import com.tourswitch.global.config.security.RefreshTokenCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieManager {

    private final RefreshTokenCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    /**
     * Refresh Token 쿠키 생성
     */
    public ResponseCookie createRefreshTokenCookie(
        String refreshToken
    ) {
        return ResponseCookie.from(
                cookieProperties.name(),
                refreshToken
            )
            .httpOnly(true)
            .secure(cookieProperties.secure())
            .sameSite(cookieProperties.sameSite())
            .path(cookieProperties.path())
            .maxAge(
                Duration.ofSeconds(
                    jwtProperties.refreshTokenExpirationSeconds()
                )
            )
            .build();
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    public ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from(
                cookieProperties.name(),
                ""
            )
            .httpOnly(true)
            .secure(cookieProperties.secure())
            .sameSite(cookieProperties.sameSite())
            .path(cookieProperties.path())
            .maxAge(Duration.ZERO)
            .build();
    }

    /**
     * 요청 쿠키에서 Refresh Token 조회
     */
    public String getRefreshToken(
        HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieProperties.name().equals(
                cookie.getName()
            )) {
                return cookie.getValue();
            }
        }

        return null;
    }
}