package com.tourswitch.domain.congestion.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MapAreaCongestionResponseDTO(
        Long areaId,
        String areaCode,
        String areaName,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
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
