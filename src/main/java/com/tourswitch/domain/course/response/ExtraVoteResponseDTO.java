package com.tourswitch.domain.course.response;

import java.util.List;
import java.util.Map;

/**
 * 추가 투표 화면 전체 상태. 역할(FOOD/LODGING/SHOPPING)별로 후보를 묶어 돌려준다.
 */
public record ExtraVoteResponseDTO(
        String roomStatus,
        Map<String, List<ExtraCandidateResponseDTO>> candidatesByRole
) {
}
