package com.tourswitch.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberRejoinTest {

    @Test
    void withdrawnMemberCanRejoinWithTheSameKakaoAccount() {
        Member member = Member.createSocialMember(
            SocialProvider.KAKAO,
            "kakao-123",
            "기존 닉네임",
            "https://example.com/old-profile.jpg"
        );
        member.withdraw();

        member.rejoin(
            "새 닉네임",
            "https://example.com/new-profile.jpg"
        );

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getNickname()).isEqualTo("새 닉네임");
        assertThat(member.getProfileImageUrl())
            .isEqualTo("https://example.com/new-profile.jpg");
        assertThat(member.getWithdrawnAt()).isNull();
    }
}
