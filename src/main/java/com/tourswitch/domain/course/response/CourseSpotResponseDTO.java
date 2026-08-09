package com.tourswitch.domain.course.response;

import com.tourswitch.domain.course.entity.CourseSpot;
import java.math.BigDecimal;

public record CourseSpotResponseDTO(
        Long id,
        Long touristSpotId,
        String spotRole,
        Integer visitOrder,
        String spotTitleSnapshot,
        BigDecimal concentrationRateSnapshot,
        Integer voteCountSnapshot,
        boolean isReplaced
) {

    public static CourseSpotResponseDTO from(CourseSpot courseSpot) {
        return new CourseSpotResponseDTO(
                courseSpot.getId(),
                courseSpot.getTouristSpotId(),
                courseSpot.getSpotRole().name(),
                courseSpot.getVisitOrder(),
                courseSpot.getSpotTitleSnapshot(),
                courseSpot.getConcentrationRateSnapshot(),
                courseSpot.getVoteCountSnapshot(),
                courseSpot.getIsReplaced());
    }
}
