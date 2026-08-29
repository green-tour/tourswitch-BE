package com.tourswitch.domain.auth.service;

import com.tourswitch.domain.auth.client.KakaoOAuthClient;
import com.tourswitch.domain.auth.exception.KakaoOAuthException;
import com.tourswitch.domain.auth.request.KakaoLoginRequestDTO;
import com.tourswitch.domain.auth.response.KakaoTokenResponseDTO;
import com.tourswitch.domain.auth.response.KakaoUserResponseDTO;
import com.tourswitch.global.config.security.JwtProperties;
import com.tourswitch.global.security.jwt.JwtProvider;
import com.tourswitch.global.security.jwt.RefreshTokenHasher;
import com.tourswitch.global.security.jwt.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final LoginMemberService loginMemberService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;

    /**
     * 카카오 로그인
     */
    public AuthLoginResult login(
        KakaoLoginRequestDTO request
    ) {
        KakaoTokenResponseDTO tokenResponse =
            kakaoOAuthClient.getToken(
                request.oauthCode(),
                request.redirectUri()
            );

        KakaoUserResponseDTO userResponse =
            kakaoOAuthClient.getUserInfo(
                tokenResponse.accessToken()
            );

        String socialId =
            userResponse.id().toString();

        String nickname =
            extractNickname(userResponse);

        LoginMemberResult loginMemberResult =
            findOrCreateLoginMember(
                socialId,
                nickname
            );

        Long memberId =
            loginMemberResult.member().getId();

        TokenPair tokenPair =
            jwtProvider.createTokenPair(memberId);

        String refreshTokenHash =
            refreshTokenHasher.hash(
                tokenPair.refreshToken()
            );

        loginMemberService.saveRefreshToken(
            memberId,
            refreshTokenHash,
            tokenPair.refreshTokenExpiresAt()
        );

        return new AuthLoginResult(
            loginMemberResult.member(),
            loginMemberResult.newUser(),
            tokenPair
        );
    }

    private String extractNickname(
        KakaoUserResponseDTO userResponse
    ) {
        if (userResponse.kakaoAccount() == null
            || userResponse.kakaoAccount().profile() == null
            || userResponse.kakaoAccount()
            .profile()
            .nickname() == null
            || userResponse.kakaoAccount()
            .profile()
            .nickname()
            .isBlank()) {

            throw new KakaoOAuthException();
        }

        return userResponse.kakaoAccount()
            .profile()
            .nickname();
    }

    /**
     * 카카오 회원 조회 또는 생성
     * 동시 로그인으로 UNIQUE 충돌 시 기존 회원 재조회
     */
    private LoginMemberResult findOrCreateLoginMember(
        String socialId,
        String nickname
    ) {
        try {
            return loginMemberService.findOrCreateKakaoMember(
                socialId,
                nickname
            );

        } catch (DataIntegrityViolationException exception) {

            return loginMemberService
                .findExistingKakaoMember(socialId)
                .orElseThrow(() -> exception);
        }
    }
}