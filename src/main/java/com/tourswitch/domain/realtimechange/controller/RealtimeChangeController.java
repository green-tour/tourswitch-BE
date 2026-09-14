package com.tourswitch.domain.realtimechange.controller;

import com.tourswitch.domain.realtimechange.request.CourseSpotReplacementRequestDTO;
import com.tourswitch.domain.realtimechange.response.AdministrativeDongResponseDTO;
import com.tourswitch.domain.realtimechange.response.CourseReplacementResponseDTO;
import com.tourswitch.domain.realtimechange.response.RegionResponseDTO;
import com.tourswitch.domain.realtimechange.response.ReplacementCandidatesResponseDTO;
import com.tourswitch.domain.realtimechange.service.CourseReplacementService;
import com.tourswitch.domain.realtimechange.service.RealtimeChangeQueryService;
import com.tourswitch.global.response.GlobalRes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * memberId 요청 파라미터는 회원/JWT 도메인이 연결되기 전까지 사용하는 임시 경계다.
 */
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RealtimeChangeController {

    private final RealtimeChangeQueryService realtimeChangeQueryService;
    private final CourseReplacementService courseReplacementService;

    @GetMapping("/regions")
    public GlobalRes<List<RegionResponseDTO>> getRegions() {
        return GlobalRes.success(realtimeChangeQueryService.getSeoulDistricts());
    }

    @GetMapping("/regions/{regionId}/administrative-dongs")
    public GlobalRes<List<AdministrativeDongResponseDTO>> getAdministrativeDongs(@PathVariable Long regionId) {
        return GlobalRes.success(realtimeChangeQueryService.getAdministrativeDongs(regionId));
    }

    @GetMapping("/courses/{courseId}/replacement-candidates")
    public GlobalRes<ReplacementCandidatesResponseDTO> getReplacementCandidates(
            @PathVariable Long courseId,
            @RequestParam Long administrativeDongId,
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return GlobalRes.success(realtimeChangeQueryService.getReplacementCandidates(
                courseId, administrativeDongId, memberId, limit));
    }

    @PatchMapping("/courses/{courseId}/spots/{courseSpotId}/replacement")
    public GlobalRes<CourseReplacementResponseDTO> replaceCourseSpot(
            @PathVariable Long courseId,
            @PathVariable Long courseSpotId,
            @RequestParam Long memberId,
            @RequestBody @Valid CourseSpotReplacementRequestDTO request) {
        return GlobalRes.success(courseReplacementService.replace(courseId, courseSpotId, memberId, request));
    }
}
