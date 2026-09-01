package com.tourswitch.domain.realtimechange.service;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseStatus;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import com.tourswitch.domain.realtimechange.exception.RealtimeChangeAccessDeniedException;
import com.tourswitch.domain.realtimechange.exception.RealtimeChangeConflictException;
import com.tourswitch.domain.realtimechange.exception.RealtimeChangeNotFoundException;
import com.tourswitch.domain.realtimechange.repository.AdministrativeDongRepository;
import com.tourswitch.domain.realtimechange.repository.RegionQueryRepository;
import com.tourswitch.domain.realtimechange.repository.ReplacementCandidateQueryRepository;
import com.tourswitch.domain.realtimechange.response.AdministrativeDongResponseDTO;
import com.tourswitch.domain.realtimechange.response.RegionResponseDTO;
import com.tourswitch.domain.realtimechange.response.ReplacementCandidatesResponseDTO;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtimeChangeQueryService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final RegionQueryRepository regionQueryRepository;
    private final AdministrativeDongRepository administrativeDongRepository;
    private final CourseRepository courseRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;
    private final ReplacementCandidateQueryRepository replacementCandidateQueryRepository;

    public List<RegionResponseDTO> getSeoulDistricts() {
        return regionQueryRepository.findAllSeoulDistricts().stream().map(RegionResponseDTO::from).toList();
    }

    public List<AdministrativeDongResponseDTO> getAdministrativeDongs(Long regionId) {
        if (!regionQueryRepository.existsById(regionId)) {
            throw new RealtimeChangeNotFoundException("존재하지 않는 자치구입니다.");
        }
        return administrativeDongRepository.findByRegionIdAndIsActiveTrueOrderByDongNameAsc(regionId).stream()
                .map(AdministrativeDongResponseDTO::from)
                .toList();
    }

    public ReplacementCandidatesResponseDTO getReplacementCandidates(Long courseId, Long administrativeDongId,
                                                                      Long memberId, int limit) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RealtimeChangeNotFoundException("존재하지 않는 코스입니다."));
        validateParticipant(course, memberId);
        validateReplaceableCourse(course);

        AdministrativeDong dong = administrativeDongRepository.findByIdAndIsActiveTrue(administrativeDongId)
                .orElseThrow(() -> new RealtimeChangeNotFoundException("존재하지 않거나 비활성화된 행정동입니다."));
        return ReplacementCandidatesResponseDTO.of(
                dong,
                replacementCandidateQueryRepository.findCandidates(courseId, administrativeDongId, limit));
    }

    private void validateParticipant(Course course, Long memberId) {
        if (!roomParticipantQueryRepository.isParticipant(course.getTravelRoomId(), memberId)) {
            throw new RealtimeChangeAccessDeniedException("해당 여행방 참여자만 대체 후보를 조회할 수 있습니다.");
        }
    }

    private void validateReplaceableCourse(Course course) {
        if (course.getStatus() != CourseStatus.CONFIRMED) {
            throw new RealtimeChangeConflictException("확정된 코스만 장소를 교체할 수 있습니다.");
        }
        if (!course.getTravelDate().isEqual(LocalDate.now(SEOUL_ZONE_ID))) {
            throw new RealtimeChangeConflictException("장소 교체는 여행 당일에만 이용할 수 있습니다.");
        }
    }
}
