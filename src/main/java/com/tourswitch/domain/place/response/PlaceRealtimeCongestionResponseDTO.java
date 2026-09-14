package com.tourswitch.domain.place.response;

import java.time.LocalDateTime;

public record PlaceRealtimeCongestionResponseDTO(
        String level,
        String message,
        Integer populationMin,
        Integer populationMax,
        LocalDateTime observedAt,
        LocalDateTime collectedAt,
        boolean delayed
) {
}
