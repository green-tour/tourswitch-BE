package com.tourswitch.domain.realtimechange.repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 선택한 행정동의 대표 좌표에서 3km 이내인 대체 후보 조회 결과 한 건.
 * 후보는 요청 시점마다 계산하므로 별도 엔티티로 저장하지 않는다.
 */
public record ReplacementCandidateRow(
        Long touristSpotId,
        String title,
        String address,
        int distanceMeters,
        List<String> matchedKeywords,
        String crowdGrade,
        LocalDateTime crowdObservedAt
) {
}
