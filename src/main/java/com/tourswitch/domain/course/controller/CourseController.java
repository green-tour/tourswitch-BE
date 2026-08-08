package com.tourswitch.domain.course.controller;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.response.CourseExtraCandidateResponseDTO;
import com.tourswitch.domain.course.response.CourseResponseDTO;
import com.tourswitch.domain.course.response.CourseSpotResponseDTO;
import com.tourswitch.domain.course.service.CourseConfirmationService;
import com.tourswitch.domain.course.service.CourseQueryService;
import com.tourswitch.global.response.GlobalRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * memberId를 요청 파라미터로 받는 것은 임시 조치다(계획 문서 6절-1 경계, VoteController와 동일한 사유).
 */
@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseQueryService courseQueryService;
    private final CourseConfirmationService courseConfirmationService;

    @GetMapping("/api/rooms/{roomId}/course")
    public GlobalRes<CourseResponseDTO> getCourse(@PathVariable Long roomId, @RequestParam Long memberId) {
        return GlobalRes.success(toResponse(courseQueryService.getCourseByTravelRoomId(roomId, memberId)));
    }

    @PatchMapping("/api/courses/{courseId}/status")
    public GlobalRes<CourseResponseDTO> confirmCourse(@PathVariable Long courseId,
                                                       @RequestParam Long memberId) {
        Course course = courseConfirmationService.confirmCourse(courseId, memberId);
        return GlobalRes.success(toResponse(course));
    }

    private CourseResponseDTO toResponse(Course course) {
        List<CourseSpotResponseDTO> stops = courseQueryService.getStops(course.getId()).stream()
                .map(CourseSpotResponseDTO::from)
                .toList();
        List<CourseExtraCandidateResponseDTO> extraCandidates = courseQueryService.getExtraCandidates(course.getId())
                .stream()
                .map(CourseExtraCandidateResponseDTO::from)
                .toList();
        return CourseResponseDTO.of(course, stops, extraCandidates);
    }
}
