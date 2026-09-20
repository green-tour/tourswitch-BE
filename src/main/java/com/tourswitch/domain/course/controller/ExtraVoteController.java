package com.tourswitch.domain.course.controller;

import com.tourswitch.domain.course.response.ExtraVoteResponseDTO;
import com.tourswitch.domain.course.service.ExtraVoteService;
import com.tourswitch.global.response.GlobalRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추가 투표 라운드 API. 관광지 투표와 같은 방 단위 경로를 쓴다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/extra-votes")
public class ExtraVoteController {

    private final ExtraVoteService extraVoteService;

    @GetMapping
    public GlobalRes<ExtraVoteResponseDTO> getCandidates(@PathVariable Long roomId,
                                                           @RequestParam Long memberId) {
        return GlobalRes.success(extraVoteService.getCandidates(roomId, memberId));
    }

    @PostMapping("/{candidateId}")
    public GlobalRes<ExtraVoteResponseDTO> vote(@PathVariable Long roomId, @PathVariable Long candidateId,
                                                  @RequestParam Long memberId) {
        return GlobalRes.success(extraVoteService.vote(roomId, candidateId, memberId));
    }

    @DeleteMapping("/{candidateId}")
    public GlobalRes<ExtraVoteResponseDTO> cancelVote(@PathVariable Long roomId, @PathVariable Long candidateId,
                                                        @RequestParam Long memberId) {
        return GlobalRes.success(extraVoteService.cancelVote(roomId, candidateId, memberId));
    }

    @PatchMapping("/completion")
    public GlobalRes<ExtraVoteResponseDTO> complete(@PathVariable Long roomId, @RequestParam Long memberId) {
        return GlobalRes.success(extraVoteService.complete(roomId, memberId));
    }
}
