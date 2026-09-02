package com.tourswitch.global.security.oauth2;

import com.tourswitch.domain.auth.service.KakaoOAuth2Service;
import com.tourswitch.domain.auth.service.LoginMemberResult;
import com.tourswitch.domain.member.exception.WithdrawnMemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
    implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final KakaoOAuth2Service kakaoOAuth2Service;

    private final DefaultOAuth2UserService delegate =
        new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
        throws OAuth2AuthenticationException {

        String registrationId =
            userRequest
                .getClientRegistration()
                .getRegistrationId();

        if (!"kakao".equals(registrationId)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("unsupported_provider"),
                "지원하지 않는 소셜 로그인 제공자입니다."
            );
        }

        OAuth2User oauth2User =
            delegate.loadUser(userRequest);

        try {
            LoginMemberResult loginResult =
                kakaoOAuth2Service.login(oauth2User);

            return new OAuth2MemberPrincipal(
                loginResult.member().getId(),
                loginResult.newUser(),
                oauth2User
            );

        } catch (WithdrawnMemberException exception) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("withdrawn_member"),
                "탈퇴한 회원입니다.",
                exception
            );
        }
    }
}