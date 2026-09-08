package com.tourswitch.global.security.jwt;

import com.tourswitch.global.config.security.JwtProperties;
import com.tourswitch.global.security.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        byte[] keyBytes =
            Decoders.BASE64.decode(jwtProperties.secret());

        this.signingKey =
            Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token + Refresh Token 발급
     */
    public TokenPair createTokenPair(Long memberId) {
        Instant now = Instant.now();

        Instant accessTokenExpiration =
            now.plusSeconds(
                jwtProperties.accessTokenExpirationSeconds()
            );

        Instant refreshTokenExpiration =
            now.plusSeconds(
                jwtProperties.refreshTokenExpirationSeconds()
            );

        String accessToken = createToken(
            memberId,
            ACCESS_TOKEN_TYPE,
            now,
            accessTokenExpiration
        );

        String refreshToken = createToken(
            memberId,
            REFRESH_TOKEN_TYPE,
            now,
            refreshTokenExpiration
        );

        LocalDateTime refreshTokenExpiresAt =
            LocalDateTime.ofInstant(
                refreshTokenExpiration,
                ZoneId.systemDefault()
            );

        return new TokenPair(
            accessToken,
            refreshToken,
            jwtProperties.accessTokenExpirationSeconds(),
            refreshTokenExpiresAt
        );
    }

    /**
     * Access Token 검증
     */
    public boolean validateAccessToken(String token) {
        return validateToken(
            token,
            ACCESS_TOKEN_TYPE
        );
    }

    /**
     * Refresh Token 검증
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(
            token,
            REFRESH_TOKEN_TYPE
        );
    }

    /**
     * JWT에서 회원 ID 추출
     */
    public Long getMemberId(String token) {
        try {
            Claims claims = parseClaims(token);

            String subject = claims.getSubject();

            if (subject == null || subject.isBlank()) {
                throw new TokenException();
            }

            return Long.valueOf(subject);

        } catch (JwtException | IllegalArgumentException exception) {
            throw new TokenException();
        }
    }

    /**
     * JWT 생성
     */
    private String createToken(
        Long memberId,
        String tokenType,
        Instant issuedAt,
        Instant expiration
    ) {
        return Jwts.builder()
            .subject(memberId.toString())
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }

    /**
     * JWT 유효성 및 토큰 종류 검증
     */
    private boolean validateToken(
        String token,
        String expectedTokenType
    ) {
        try {
            Claims claims = parseClaims(token);

            String tokenType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
            );

            return expectedTokenType.equals(tokenType);
        } catch (
            JwtException
            | IllegalArgumentException exception
        ) {
            return false;
        }
    }

    /**
     * JWT Claims 파싱
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}