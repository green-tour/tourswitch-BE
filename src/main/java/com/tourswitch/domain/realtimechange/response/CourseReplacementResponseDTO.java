package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.entity.CourseReplacement;
import java.time.LocalDateTime;

public record CourseReplacementResponseDTO(
        Long replacementId,
        Long courseId,
        Long courseSpotId,
        Long administrativeDongId,
        Long previousTouristSpotId,
        Long replacementTouristSpotId,
        int radiusMeters,
        LocalDateTime replacedAt
) {

    public static CourseReplacementResponseDTO from(CourseReplacement replacement) {
        return new CourseReplacementResponseDTO(
                replacement.getId(),
                replacement.getCourseId(),
                replacement.getCourseSpotId(),
                replacement.getAdministrativeDongId(),
                replacement.getPreviousTouristSpotId(),
                replacement.getReplacementTouristSpotId(),
                replacement.getRadiusMeters(),
                replacement.getReplacedAt());
    }
}
