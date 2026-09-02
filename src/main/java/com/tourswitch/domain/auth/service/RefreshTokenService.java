package com.tourswitch.domain.auth.service;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.domain.member.entity.MemberStatus;
import com.tourswitch.domain.member.exception.MemberNotFoundException;
import com.tourswitch.domain.member.exception.WithdrawnMemberException;
import com.tourswitch.domain.member.repository.MemberRepository;
import com.tourswitch.global.security.exception.TokenException;
import com.tourswitch.global.security.jwt.JwtProvider;
import com.tourswitch.global.security.jwt.RefreshTokenHasher;
import com.tourswitch.global.security.jwt.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;

    /**
     * Refresh Token 검증 및 토큰 재발급
     */
    @Transactional
    public TokenPair refresh(String refreshToken) {

        validateRefreshToken(refreshToken);

        Long memberId =
            jwtProvider.getMemberId(refreshToken);

        Member member = memberRepository.findById(memberId)
            .orElseThrow(TokenException::new);

        validateActiveMember(member);
        validateStoredRefreshToken(member, refreshToken);

        TokenPair newTokenPair =
            jwtProvider.createTokenPair(memberId);

        updateRefreshToken(
            member,
            newTokenPair
        );

        return newTokenPair;
    }

    /**
     * JWT 자체 검증
     */
    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null
            || refreshToken.isBlank()
            || !jwtProvider.validateRefreshToken(refreshToken)) {

            throw new TokenException();
        }
    }

    /**
     * 회원 상태 검증
     */
    private void validateActiveMember(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new WithdrawnMemberException();
        }
    }

    /**
     * DB에 저장된 Refresh Token과 비교
     */
    private void validateStoredRefreshToken(
        Member member,
        String refreshToken
    ) {
        if (member.getRefreshTokenHash() == null
            || member.getRefreshTokenExpiresAt() == null) {

            throw new TokenException();
        }

        if (member.getRefreshTokenExpiresAt()
            .isBefore(LocalDateTime.now())) {

            throw new TokenException();
        }

        String refreshTokenHash =
            refreshTokenHasher.hash(refreshToken);

        if (!refreshTokenHash.equals(
            member.getRefreshTokenHash()
        )) {
            throw new TokenException();
        }
    }

    /**
     * 새 Refresh Token 정보 저장
     */
    private void updateRefreshToken(
        Member member,
        TokenPair newTokenPair
    ) {
        String newRefreshTokenHash =
            refreshTokenHasher.hash(
                newTokenPair.refreshToken()
            );

        member.updateRefreshToken(
            newRefreshTokenHash,
            newTokenPair.refreshTokenExpiresAt()
        );
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(MemberNotFoundException::new);

        member.clearRefreshToken();
    }

    /**
     * 로그인 성공 시 Refresh Token 정보 저장
     */
    @Transactional
    public void saveRefreshToken(
        Long memberId,
        TokenPair tokenPair
    ) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(MemberNotFoundException::new);

        validateActiveMember(member);

        updateRefreshToken(
            member,
            tokenPair
        );
    }
}