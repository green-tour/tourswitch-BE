package com.tourswitch.domain.vote.service;

import java.math.BigDecimal;

/**
 * 코스 선정을 위해 코스 도메인에 노출하는 투표 결과 한 줄. 득표수 내림차순, 동률이면
 * 집중률(concentrationRateSnapshot) 오름차순으로 이미 정렬돼 있다(DB설계 8.1절/11절).
 * title/좌표는 room_candidate 스냅샷에서 그대로 오므로 코스생성 단계가 TourAPI를 재호출하지 않는다.
 */
public record CourseSelectionCandidate(
        Long roomCandidateId,
        String contentId,
        String title,
        double latitude,
        double longitude,
        long voteCount,
        BigDecimal concentrationRateSnapshot
) {
}
