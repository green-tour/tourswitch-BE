package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.entity.CourseReplacement;
import java.time.LocalDateTime;

public record CourseReplacementResponseDTO(
        Long replacementId,
        Long courseId,
        Long courseSpotId,
        Long administrativeDongId,
        String previousContentId,
        String replacementContentId,
        int radiusMeters,
        LocalDateTime replacedAt
) {

    public static CourseReplacementResponseDTO from(CourseReplacement replacement) {
        return new CourseReplacementResponseDTO(
                replacement.getId(),
                replacement.getCourseId(),
                replacement.getCourseSpotId(),
                replacement.getAdministrativeDongId(),
                replacement.getPreviousContentId(),
                replacement.getReplacementContentId(),
                replacement.getRadiusMeters(),
                replacement.getReplacedAt());
    }
}
