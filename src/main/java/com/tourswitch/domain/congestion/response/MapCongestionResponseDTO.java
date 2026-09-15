package com.tourswitch.domain.congestion.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 혼잡도 지도 생성 시각, 범례와 서울 전체 영역 목록을 묶어 제공한다.
 */
public record MapCongestionResponseDTO(
        LocalDateTime generatedAt,
        int delayedAreaCount,
        List<CongestionLegendResponseDTO> legend,
        List<MapAreaCongestionResponseDTO> areas
) {
}
