package com.tourswitch.domain.realtimechange.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record CourseSpotReplacementRequestDTO(
        @NotNull(message = "administrativeDongId는 필수입니다.") Long administrativeDongId,
        @NotBlank(message = "replacementContentId는 필수입니다.") String replacementContentId
) {
}
