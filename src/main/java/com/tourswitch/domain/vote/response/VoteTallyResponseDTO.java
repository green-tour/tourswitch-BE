package com.tourswitch.domain.vote.response;

import java.util.List;

/**
 * 투표·취소·완료 API 응답에 갱신된 집계를 함께 실어보낸다(계획 문서 3절 결정사항 1).
 * 후보별 득표수와 참여자별 완료 상태, 방 상태(전원 완료 시 CLOSED로 바뀔 수 있음)를 함께 내려준다.
 *
 * extraCandidates는 추가 투표 라운드의 음식점·숙박·쇼핑이다. 관광지 후보와 저장 위치가 달라
 * 목록을 나눠 담는다. 라운드가 시작되기 전에는 비어 있다.
 */
public record VoteTallyResponseDTO(
        String roomStatus,
        List<CandidateTallyResponseDTO> candidates,
        List<ExtraCandidateTallyResponseDTO> extraCandidates,
        List<ParticipantStatusResponseDTO> participants
) {

    public static VoteTallyResponseDTO of(String roomStatus, List<CandidateTallyResponseDTO> candidates,
                                           List<ExtraCandidateTallyResponseDTO> extraCandidates,
                                           List<ParticipantStatusResponseDTO> participants) {
        return new VoteTallyResponseDTO(roomStatus, candidates, extraCandidates, participants);
    }
}
