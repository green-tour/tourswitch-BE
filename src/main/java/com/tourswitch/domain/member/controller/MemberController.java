package com.tourswitch.domain.member.controller;

import com.tourswitch.domain.member.request.UpdateNicknameRequestDTO;
import com.tourswitch.domain.member.request.WithdrawMemberRequestDTO;
import com.tourswitch.domain.member.response.MemberResponseDTO;
import com.tourswitch.domain.member.service.MemberService;
import com.tourswitch.global.response.GlobalRes;
import com.tourswitch.global.security.cookie.RefreshTokenCookieManager;
import com.tourswitch.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class MemberController {

    private final MemberService memberService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public GlobalRes<MemberResponseDTO> getMyInfo(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        MemberResponseDTO response =
            memberService.getMyInfo(
                principal.memberId()
            );

        return GlobalRes.success(response);
    }

    /**
     * 닉네임 수정
     */
    @PatchMapping("/me")
    public GlobalRes<MemberResponseDTO> updateNickname(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateNicknameRequestDTO request
    ) {
        MemberResponseDTO response =
            memberService.updateNickname(
                principal.memberId(),
                request
            );

        return GlobalRes.success(response);
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    public GlobalRes<Void> withdraw(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody WithdrawMemberRequestDTO request,
        HttpServletResponse response
    ) {
        memberService.withdraw(
            principal.memberId()
        );

        ResponseCookie expiredRefreshTokenCookie =
            refreshTokenCookieManager
                .createExpiredRefreshTokenCookie();

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            expiredRefreshTokenCookie.toString()
        );

        return GlobalRes.success();
    }
}