package com.tourswitch.domain.course.response;

import com.tourswitch.domain.course.entity.CourseSpot;
import java.math.BigDecimal;

public record CourseSpotResponseDTO(
        Long id,
        String contentId,
        String spotRole,
        Integer visitOrder,
        String spotTitleSnapshot,
        String address,
        Double latitude,
        Double longitude,
        BigDecimal concentrationRateSnapshot,
        Integer voteCountSnapshot,
        boolean isReplaced
) {

    public static CourseSpotResponseDTO of(CourseSpot courseSpot, String address, Double latitude, Double longitude) {
        return new CourseSpotResponseDTO(
                courseSpot.getId(),
                courseSpot.getContentId(),
                courseSpot.getSpotRole().name(),
                courseSpot.getVisitOrder(),
                courseSpot.getSpotTitleSnapshot(),
                address,
                latitude,
                longitude,
                courseSpot.getConcentrationRateSnapshot(),
                courseSpot.getVoteCountSnapshot(),
                courseSpot.getIsReplaced());
    }
}
