package com.tourswitch.domain.course.response;

import com.tourswitch.domain.course.entity.CourseExtraCandidate;

public record CourseExtraCandidateResponseDTO(
        Long id,
        Long anchorCourseSpotId,
        String contentId,
        String titleSnapshot,
        String imageUrlSnapshot,
        String spotRole,
        Integer distanceMeters,
        Integer displayOrder,
        boolean isSelected
) {

    public static CourseExtraCandidateResponseDTO from(CourseExtraCandidate candidate) {
        return new CourseExtraCandidateResponseDTO(
                candidate.getId(),
                candidate.getAnchorCourseSpot().getId(),
                candidate.getContentId(),
                candidate.getTitleSnapshot(),
                candidate.getImageUrlSnapshot(),
                candidate.getSpotRole().name(),
                candidate.getDistanceMeters(),
                candidate.getDisplayOrder(),
                candidate.getIsSelected());
    }
}
