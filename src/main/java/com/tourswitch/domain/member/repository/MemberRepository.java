package com.tourswitch.domain.member.repository;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.domain.member.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySocialProviderAndSocialId(
        SocialProvider socialProvider
        , String socialId
    );
}
