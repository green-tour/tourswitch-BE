package com.tourswitch.domain.realtimechange.request;

import jakarta.validation.constraints.NotNull;

public record CourseSpotReplacementRequestDTO(
        @NotNull(message = "administrativeDongId는 필수입니다.") Long administrativeDongId,
        @NotNull(message = "replacementTouristSpotId는 필수입니다.") Long replacementTouristSpotId
) {
}
