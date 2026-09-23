package com.tourswitch.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoOAuth2Service {

    private final LoginMemberService loginMemberService;

    /**
     * 카카오 사용자 정보로 회원 조회 또는 최초 생성
     */
    public LoginMemberResult login(OAuth2User oauth2User) {
        String socialId = getSocialId(oauth2User);
        String nickname = getNickname(oauth2User);
        String profileImageUrl = getProfileImageUrl(oauth2User);

        return findOrCreateLoginMember(
            socialId,
            nickname,
            profileImageUrl
        );
    }

    /**
     * 카카오 회원 조회 또는 생성
     * 동시 최초 로그인으로 UNIQUE 충돌 시 기존 회원 재조회
     */
    private LoginMemberResult findOrCreateLoginMember(
        String socialId,
        String nickname,
        String profileImageUrl
    ) {
        try {
            return loginMemberService.findOrCreateKakaoMember(
                socialId,
                nickname,
                profileImageUrl
            );

        } catch (DataIntegrityViolationException exception) {
            return loginMemberService
                .findExistingKakaoMember(socialId)
                .orElseThrow(() -> exception);
        }
    }

    /**
     * 카카오 회원 번호 추출
     */
    private String getSocialId(OAuth2User oauth2User) {
        Object id = oauth2User.getAttribute("id");

        if (id == null) {
            throw oauth2AuthenticationException(
                "카카오 회원 번호를 확인할 수 없습니다."
            );
        }

        return String.valueOf(id);
    }

    /**
     * 카카오 닉네임 추출
     */
    private String getNickname(OAuth2User oauth2User) {
        Map<?, ?> profile = getProfile(oauth2User);

        Object nickname =
            profile.get("nickname");

        if (nickname == null
            || nickname.toString().isBlank()) {

            throw oauth2AuthenticationException(
                "카카오 닉네임을 확인할 수 없습니다."
            );
        }

        return nickname.toString();
    }

    /** 카카오 프로필 이미지 URL 추출 */
    private String getProfileImageUrl(OAuth2User oauth2User) {
        Map<?, ?> profile = getProfile(oauth2User);

        Object profileImageUrl = profile.get("profile_image_url");

        if (profileImageUrl == null
            || profileImageUrl.toString().isBlank()) {

            return null;
        }

        return profileImageUrl.toString();
    }

    private Map<?, ?> getProfile(OAuth2User oauth2User) {
        Map<String, Object> kakaoAccount =
            oauth2User.getAttribute("kakao_account");

        if (kakaoAccount == null) {
            throw oauth2AuthenticationException(
                "카카오 계정 정보를 확인할 수 없습니다."
            );
        }

        Object profileObject = kakaoAccount.get("profile");

        if (!(profileObject instanceof Map<?, ?> profile)) {
            throw oauth2AuthenticationException(
                "카카오 프로필 정보를 확인할 수 없습니다."
            );
        }

        return profile;
    }

    private OAuth2AuthenticationException oauth2AuthenticationException(
        String message
    ) {
        return new OAuth2AuthenticationException(
            new OAuth2Error("kakao_user_info_error"),
            message
        );
    }
}
