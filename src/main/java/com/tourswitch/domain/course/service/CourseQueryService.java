package com.tourswitch.domain.course.service;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseExtraCandidate;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.exception.CourseAccessDeniedException;
import com.tourswitch.domain.course.exception.CourseNotFoundException;
import com.tourswitch.domain.course.repository.CourseExtraCandidateRepository;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import com.tourswitch.domain.course.repository.CourseSpotPlaceQueryRepository;
import com.tourswitch.domain.course.repository.CourseSpotPlaceQueryRepository.CourseSpotPlaceRow;
import com.tourswitch.domain.course.response.CourseSpotResponseDTO;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final CourseSpotPlaceQueryRepository courseSpotPlaceQueryRepository;
    private final CourseExtraCandidateRepository courseExtraCandidateRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;

    public Course getCourseByTravelRoomId(Long travelRoomId, Long memberId) {
        if (!roomParticipantQueryRepository.isParticipant(travelRoomId, memberId)) {
            throw new CourseAccessDeniedException("이 방의 참여자만 이용할 수 있습니다.");
        }
        return courseRepository.findByTravelRoomId(travelRoomId)
                .orElseThrow(() -> new CourseNotFoundException("아직 생성된 코스가 없습니다."));
    }

    public Course getCourseById(Long courseId, Long memberId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("코스를 찾을 수 없습니다."));
        if (!roomParticipantQueryRepository.isParticipant(course.getTravelRoomId(), memberId)) {
            throw new CourseAccessDeniedException("이 방의 참여자만 이용할 수 있습니다.");
        }
        return course;
    }

    public List<CourseSpot> getStops(Long courseId) {
        return courseSpotRepository.findByCourseIdOrderByVisitOrderAsc(courseId);
    }

    public List<CourseSpotResponseDTO> getStopResponses(Long courseId) {
        Map<Long, CourseSpotPlaceRow> placesByCourseSpotId = courseSpotPlaceQueryRepository.findByCourseId(courseId)
                .stream()
                .collect(Collectors.toMap(CourseSpotPlaceRow::courseSpotId, Function.identity()));
        return getStops(courseId).stream()
                .map(courseSpot -> toResponse(courseSpot, placesByCourseSpotId.get(courseSpot.getId())))
                .toList();
    }

    public List<CourseExtraCandidate> getExtraCandidates(Long courseId) {
        return courseExtraCandidateRepository.findByCourseId(courseId);
    }

    private CourseSpotResponseDTO toResponse(CourseSpot courseSpot, CourseSpotPlaceRow place) {
        if (place == null) {
            return CourseSpotResponseDTO.of(courseSpot, null, null, null);
        }
        return CourseSpotResponseDTO.of(
                courseSpot, place.address(), place.latitude(), place.longitude());
    }
}
