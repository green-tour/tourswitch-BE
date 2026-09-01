package com.tourswitch.domain.realtimechange.service;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.entity.CourseStatus;
import com.tourswitch.domain.course.entity.SpotRole;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import com.tourswitch.domain.realtimechange.entity.CourseReplacement;
import com.tourswitch.domain.realtimechange.exception.InvalidReplacementRequestException;
import com.tourswitch.domain.realtimechange.exception.RealtimeChangeAccessDeniedException;
import com.tourswitch.domain.realtimechange.exception.RealtimeChangeConflictException;
import com.tourswitch.domain.realtimechange.exception.RealtimeChangeNotFoundException;
import com.tourswitch.domain.realtimechange.repository.AdministrativeDongRepository;
import com.tourswitch.domain.realtimechange.repository.CourseReplacementRepository;
import com.tourswitch.domain.realtimechange.repository.ReplacementCandidateQueryRepository;
import com.tourswitch.domain.realtimechange.repository.ReplacementCandidateRow;
import com.tourswitch.domain.realtimechange.request.CourseSpotReplacementRequestDTO;
import com.tourswitch.domain.realtimechange.response.CourseReplacementResponseDTO;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseReplacementService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final AdministrativeDongRepository administrativeDongRepository;
    private final CourseReplacementRepository courseReplacementRepository;
    private final ReplacementCandidateQueryRepository replacementCandidateQueryRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;

    public CourseReplacementResponseDTO replace(Long courseId, Long courseSpotId, Long memberId,
                                                CourseSpotReplacementRequestDTO request) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new RealtimeChangeNotFoundException("존재하지 않는 코스입니다."));
        validateParticipant(course, memberId);
        validateReplaceableCourse(course);

        if (courseReplacementRepository.existsByCourseId(courseId)) {
            throw new RealtimeChangeConflictException("이 코스는 이미 한 번 장소를 교체했습니다.");
        }

        CourseSpot courseSpot = courseSpotRepository.findByIdAndCourseId(courseSpotId, courseId)
                .orElseThrow(() -> new RealtimeChangeNotFoundException("코스에 포함되지 않은 장소입니다."));
        if (courseSpot.getSpotRole() != SpotRole.ATTRACTION) {
            throw new InvalidReplacementRequestException("관광지 역할의 장소만 교체할 수 있습니다.");
        }

        AdministrativeDong dong = administrativeDongRepository.findByIdAndIsActiveTrue(
                        request.administrativeDongId())
                .orElseThrow(() -> new RealtimeChangeNotFoundException("존재하지 않거나 비활성화된 행정동입니다."));
        ReplacementCandidateRow candidate = replacementCandidateQueryRepository.findEligibleCandidate(
                        courseId, dong.getId(), request.replacementTouristSpotId())
                .orElseThrow(() -> new InvalidReplacementRequestException(
                        "선택한 장소가 3km·여행방 키워드 조건을 만족하지 않습니다."));

        Long previousTouristSpotId = courseSpot.getTouristSpotId();
        courseSpot.replaceWith(candidate.touristSpotId(), candidate.title(), LocalDateTime.now(SEOUL_ZONE_ID));

        CourseReplacement replacement = CourseReplacement.create(
                courseId,
                courseSpotId,
                dong.getId(),
                previousTouristSpotId,
                candidate.touristSpotId(),
                memberId);
        return CourseReplacementResponseDTO.from(courseReplacementRepository.saveAndFlush(replacement));
    }

    private void validateParticipant(Course course, Long memberId) {
        if (!roomParticipantQueryRepository.isParticipant(course.getTravelRoomId(), memberId)) {
            throw new RealtimeChangeAccessDeniedException("해당 여행방 참여자만 장소를 교체할 수 있습니다.");
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
