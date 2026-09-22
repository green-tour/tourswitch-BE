package com.tourswitch.domain.vote.controller;

import com.tourswitch.global.security.principal.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.tourswitch.domain.vote.request.CompletionRequestDTO;
import com.tourswitch.domain.vote.response.VoteTallyResponseDTO;
import com.tourswitch.domain.vote.service.VoteService;
import com.tourswitch.global.response.GlobalRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PutMapping("/votes/{candidateId}")
    public GlobalRes<VoteTallyResponseDTO> selectCandidate(@PathVariable Long roomId,
                                                            @PathVariable Long candidateId,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(voteService.selectCandidate(roomId, candidateId, principal.memberId()));
    }

    @DeleteMapping("/votes/{candidateId}")
    public GlobalRes<VoteTallyResponseDTO> cancelVote(@PathVariable Long roomId,
                                                       @PathVariable Long candidateId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(voteService.cancelVote(roomId, candidateId, principal.memberId()));
    }

    @PatchMapping("/participants/me/completion")
    public GlobalRes<VoteTallyResponseDTO> updateCompletion(@PathVariable Long roomId,
                                                             @RequestBody @Valid CompletionRequestDTO request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(voteService.completeSelection(roomId, principal.memberId(), request.completed()));
    }

    @PatchMapping("/revote")
    public GlobalRes<VoteTallyResponseDTO> startRevote(@PathVariable Long roomId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(voteService.startRevote(roomId, principal.memberId()));
    }

    /**
     * 방장이 남은 참여자를 기다리지 않고 현재 라운드를 끝낸다.
     * 관광지 투표 중이면 추가 투표로, 추가 투표 중이면 코스 확정으로 넘어간다.
     */
    @PatchMapping("/close")
    public GlobalRes<VoteTallyResponseDTO> closeByHost(@PathVariable Long roomId, @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(voteService.closeCurrentRoundByHost(roomId, principal.memberId()));
    }

    @GetMapping("/votes/tally")
    public GlobalRes<VoteTallyResponseDTO> getTally(@PathVariable Long roomId, @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(voteService.getTally(roomId, principal.memberId()));
    }
}
