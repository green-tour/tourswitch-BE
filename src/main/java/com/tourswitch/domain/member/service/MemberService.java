package com.tourswitch.domain.member.service;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.domain.member.entity.MemberStatus;
import com.tourswitch.domain.member.exception.MemberNotFoundException;
import com.tourswitch.domain.member.exception.WithdrawnMemberException;
import com.tourswitch.domain.member.repository.MemberRepository;
import com.tourswitch.domain.member.request.UpdateNicknameRequestDTO;
import com.tourswitch.domain.member.response.MemberResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 내 정보 조회
     */
    public MemberResponseDTO getMyInfo(Long memberId) {
        Member member = getActiveMember(memberId);

        return MemberResponseDTO.from(member);
    }

    /**
     * 닉네임 수정
     */
    @Transactional
    public MemberResponseDTO updateNickname(
        Long memberId,
        UpdateNicknameRequestDTO request
    ) {
        Member member = getActiveMember(memberId);

        member.updateNickname(request.nickname());

        return MemberResponseDTO.from(member);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = getActiveMember(memberId);

        member.withdraw();
    }

    /**
     * 정상 회원 조회
     */
    private Member getActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(MemberNotFoundException::new);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new WithdrawnMemberException();
        }

        return member;
    }
}