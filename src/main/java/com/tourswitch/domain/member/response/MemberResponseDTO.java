package com.tourswitch.domain.member.response;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.domain.member.entity.SocialProvider;

import java.time.LocalDateTime;

public record MemberResponseDTO(
    Long id
    , String nickname
    , SocialProvider socialProvider
    , LocalDateTime createdAt
) {
    public static MemberResponseDTO from(Member member) {
        return new MemberResponseDTO(
            member.getId(),
            member.getNickname(),
            member.getSocialProvider(),
            member.getCreatedAt()
        );
    }
}
