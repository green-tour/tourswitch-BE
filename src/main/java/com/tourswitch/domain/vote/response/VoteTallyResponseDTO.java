package com.tourswitch.domain.vote.response;

import java.util.List;

/**
 * 투표·취소·완료 API 응답에 갱신된 집계를 함께 실어보낸다(계획 문서 3절 결정사항 1).
 * 후보별 득표수와 참여자별 완료 상태, 방 상태(전원 완료 시 CLOSED로 바뀔 수 있음)를 함께 내려준다.
 */
public record VoteTallyResponseDTO(
        String roomStatus,
        List<CandidateTallyResponseDTO> candidates,
        List<ParticipantStatusResponseDTO> participants
) {

    public static VoteTallyResponseDTO of(String roomStatus, List<CandidateTallyResponseDTO> candidates,
                                           List<ParticipantStatusResponseDTO> participants) {
        return new VoteTallyResponseDTO(roomStatus, candidates, participants);
    }
}
