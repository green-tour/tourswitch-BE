package com.tourswitch.domain.congestion.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 서울 실시간 영역 하나의 중심점, 폴리곤, 최신 혼잡도와 관광지 목록을 제공한다.
 */
public record MapAreaCongestionResponseDTO(
        Long areaId,
        String areaCode,
        String areaName,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
        GeoJsonPolygonResponseDTO boundary,
        String congestionLevel,
        String color,
        String congestionMessage,
        Integer populationMin,
        Integer populationMax,
        LocalDateTime observedAt,
        LocalDateTime collectedAt,
        boolean delayed,
        List<MapPlaceResponseDTO> places
) {
}
