package com.tourswitch.domain.congestion.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 서울 실시간 영역과 해당 영역의 최신 혼잡도 조회 결과를 표현한다.
 */
public record MapCongestionSnapshot(
        Long areaId,
        String areaCode,
        String areaName,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryGeoJson,
        String congestionLevel,
        String congestionMessage,
        Integer populationMin,
        Integer populationMax,
        LocalDateTime observedAt,
        LocalDateTime collectedAt
) {
}
