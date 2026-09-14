package com.tourswitch.domain.congestion.response;

import java.time.LocalDateTime;
import java.util.List;

public record MapCongestionResponseDTO(
        LocalDateTime generatedAt,
        int delayedAreaCount,
        List<CongestionLegendResponseDTO> legend,
        List<MapAreaCongestionResponseDTO> areas
) {
}
