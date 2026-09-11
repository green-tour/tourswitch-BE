package com.tourswitch.domain.room.controller;

import com.tourswitch.domain.room.response.InviteInfoResponse;
import com.tourswitch.domain.room.response.JoinRoomResponse;
import com.tourswitch.domain.room.service.InviteService;
import com.tourswitch.global.response.GlobalRes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invites")
public class InviteController {
    private final InviteService inviteService;

    @GetMapping("/{inviteToken}")
    public GlobalRes<InviteInfoResponse> getInvite(@PathVariable @NotBlank String inviteToken) {
        return GlobalRes.success(inviteService.getInvite(inviteToken));
    }

    @PostMapping("/{inviteToken}/participants")
    public GlobalRes<JoinRoomResponse> join(@PathVariable @NotBlank String inviteToken,
                                            @RequestParam @Positive Long memberId) {
        return GlobalRes.success(inviteService.join(inviteToken, memberId));
    }
}
