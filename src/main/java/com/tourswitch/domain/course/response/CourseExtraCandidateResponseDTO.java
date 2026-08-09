package com.tourswitch.domain.course.response;

import com.tourswitch.domain.course.entity.CourseExtraCandidate;

public record CourseExtraCandidateResponseDTO(
        Long id,
        Long anchorCourseSpotId,
        Long touristSpotId,
        String spotRole,
        Integer distanceMeters,
        Integer displayOrder,
        boolean isSelected
) {

    public static CourseExtraCandidateResponseDTO from(CourseExtraCandidate candidate) {
        return new CourseExtraCandidateResponseDTO(
                candidate.getId(),
                candidate.getAnchorCourseSpot().getId(),
                candidate.getTouristSpotId(),
                candidate.getSpotRole().name(),
                candidate.getDistanceMeters(),
                candidate.getDisplayOrder(),
                candidate.getIsSelected());
    }
}
