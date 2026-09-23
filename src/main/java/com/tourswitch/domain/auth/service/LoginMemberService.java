package com.tourswitch.domain.auth.service;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.domain.member.entity.MemberStatus;
import com.tourswitch.domain.member.entity.SocialProvider;
import com.tourswitch.domain.member.exception.WithdrawnMemberException;
import com.tourswitch.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginMemberService {

    private final MemberRepository memberRepository;

    /**
     * 카카오 회원 조회 또는 최초 생성
     */
    @Transactional
    public LoginMemberResult findOrCreateKakaoMember(
        String socialId,
        String nickname,
        String profileImageUrl
    ) {
        return memberRepository.findBySocialProviderAndSocialId(
                SocialProvider.KAKAO,
                socialId
            )
            .map(member -> restoreOrValidateExistingMember(
                member,
                nickname,
                profileImageUrl
            ))
            .orElseGet(() ->
                createNewMember(socialId, nickname, profileImageUrl)
            );
    }

    /**
     * 기존 회원 처리
     */
    private LoginMemberResult restoreOrValidateExistingMember(
        Member member,
        String nickname,
        String profileImageUrl
    ) {
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            member.rejoin(nickname, profileImageUrl);

            return new LoginMemberResult(member, false);
        }

        validateActiveMember(member);
        member.updateProfileImageUrl(profileImageUrl);

        return new LoginMemberResult(
            member,
            false
        );
    }

    /**
     * 활성 회원 여부 확인
     */
    private void validateActiveMember(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new WithdrawnMemberException();
        }
    }

    /**
     * 최초 로그인 회원 생성
     */
    private LoginMemberResult createNewMember(
        String socialId,
        String nickname,
        String profileImageUrl
    ) {
        Member member = Member.createSocialMember(
            SocialProvider.KAKAO,
            socialId,
            nickname,
            profileImageUrl
        );

        Member savedMember =
            memberRepository.saveAndFlush(member);

        return new LoginMemberResult(
            savedMember,
            true
        );
    }

    /**
     * 기존 카카오 회원 조회
     */
    public Optional<LoginMemberResult> findExistingKakaoMember(
        String socialId
    ) {
        return memberRepository.findBySocialProviderAndSocialId(
            SocialProvider.KAKAO,
            socialId
        )
        .map(member -> {
            validateActiveMember(member);
            return new LoginMemberResult(member, false);
        });
    }
}
