package com.tourswitch.domain.history.controller;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.response.*;
import com.tourswitch.domain.course.service.CourseQueryService;
import com.tourswitch.domain.history.response.TravelHistoryItemResponse;
import com.tourswitch.domain.history.service.TravelHistoryService;
import com.tourswitch.global.response.GlobalRes;
import com.tourswitch.global.response.PageRes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class TravelHistoryController {
    private final TravelHistoryService travelHistoryService;
    private final CourseQueryService courseQueryService;

    @GetMapping
    public GlobalRes<PageRes<TravelHistoryItemResponse>> getHistories(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return GlobalRes.success(travelHistoryService.getHistories(memberId, page, size));
    }

    @GetMapping("/{courseId}")
    public GlobalRes<CourseResponseDTO> getHistory(@PathVariable Long courseId, @RequestParam Long memberId) {
        Course course = courseQueryService.getCourseById(courseId, memberId);
        List<CourseSpotResponseDTO> stops = courseQueryService.getStops(courseId).stream()
                .map(CourseSpotResponseDTO::from).toList();
        List<CourseExtraCandidateResponseDTO> extras = courseQueryService.getExtraCandidates(courseId).stream()
                .map(CourseExtraCandidateResponseDTO::from).toList();
        return GlobalRes.success(CourseResponseDTO.of(course, stops, extras));
    }
}
