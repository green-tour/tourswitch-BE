package com.tourswitch.domain.vote.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * memberId를 요청 파라미터로 받는 것은 임시 조치다. 회원 도메인의 JWT 인증이 배선되면
 * SecurityContext에서 인증 주체를 꺼내는 방식으로 교체해야 한다(계획 문서 6절-1, 6절-3 경계).
 */
@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PutMapping("/votes/{candidateId}")
    public GlobalRes<VoteTallyResponseDTO> selectCandidate(@PathVariable Long roomId,
                                                            @PathVariable Long candidateId,
                                                            @RequestParam Long memberId) {
        return GlobalRes.success(voteService.selectCandidate(roomId, candidateId, memberId));
    }

    @DeleteMapping("/votes/{candidateId}")
    public GlobalRes<VoteTallyResponseDTO> cancelVote(@PathVariable Long roomId,
                                                       @PathVariable Long candidateId,
                                                       @RequestParam Long memberId) {
        return GlobalRes.success(voteService.cancelVote(roomId, candidateId, memberId));
    }

    @PatchMapping("/participants/me/completion")
    public GlobalRes<VoteTallyResponseDTO> updateCompletion(@PathVariable Long roomId,
                                                             @RequestBody @Valid CompletionRequestDTO request,
                                                             @RequestParam Long memberId) {
        return GlobalRes.success(voteService.completeSelection(roomId, memberId, request.completed()));
    }

    @GetMapping("/votes/tally")
    public GlobalRes<VoteTallyResponseDTO> getTally(@PathVariable Long roomId, @RequestParam Long memberId) {
        return GlobalRes.success(voteService.getTally(roomId, memberId));
    }
}
