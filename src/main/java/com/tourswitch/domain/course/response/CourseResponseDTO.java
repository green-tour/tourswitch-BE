package com.tourswitch.domain.course.response;

import com.tourswitch.domain.course.entity.Course;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CourseResponseDTO(
        Long id,
        Long travelRoomId,
        LocalDate travelDate,
        Integer totalDistanceMeters,
        String status,
        LocalDateTime confirmedAt,
        List<CourseSpotResponseDTO> stops,
        List<CourseExtraCandidateResponseDTO> extraCandidates
) {

    public static CourseResponseDTO of(Course course, List<CourseSpotResponseDTO> stops,
                                        List<CourseExtraCandidateResponseDTO> extraCandidates) {
        return new CourseResponseDTO(course.getId(), course.getTravelRoomId(), course.getTravelDate(),
                course.getTotalDistanceMeters(), course.getStatus().name(), course.getConfirmedAt(), stops,
                extraCandidates);
    }
}
