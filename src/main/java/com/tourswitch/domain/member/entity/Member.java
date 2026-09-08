package com.tourswitch.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "member"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_social_provider_social_id",
            columnNames = {"social_provider", "social_id"}
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    private SocialProvider socialProvider;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "purged_at")
    private LocalDateTime purgedAt;

    /**
     * social 최초 로그인 회원 생성
     */
    public static Member createSocialMember(
        SocialProvider socialProvider,
        String socialId,
        String nickname
    ) {
        Member member = new Member();

        member.socialProvider = socialProvider;
        member.socialId = socialId;
        member.nickname = nickname;
        member.status = MemberStatus.ACTIVE;

        return member;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.nickname = "탈퇴한 회원";

        clearRefreshToken();
    }

    /**
     * Refresh Token 정보 저장
     */
    public void updateRefreshToken(
        String refreshTokenHash,
        LocalDateTime refreshTokenExpiresAt
    ) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    /**
     * Refresh Token 정보 삭제
     */
    public void clearRefreshToken() {
        this.refreshTokenHash = null;
        this.refreshTokenExpiresAt = null;
    }
}
