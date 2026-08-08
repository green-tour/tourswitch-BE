package com.tourswitch.domain.course.service;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.entity.CourseStatus;
import com.tourswitch.domain.course.entity.SpotRole;
import com.tourswitch.domain.course.exception.CourseAccessDeniedException;
import com.tourswitch.domain.course.exception.CourseNotFoundException;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import com.tourswitch.domain.course.repository.SpotDailyDemandQueryRepository;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부가 카테고리 선택(2단계) 후 방장이 코스를 확정한다(계획 문서 5단계, DB설계 8.1절/9.4절).
 * 동일 코스 재확정 요청은 멱등하게 처리하고(이미 CONFIRMED면 그대로 반환), 확정 시점에
 * spot_daily_demand를 ATTRACTION 경유지에 한해 원자적으로 갱신한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseConfirmationService {

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;
    private final TravelRoomStatusQueryRepository travelRoomStatusQueryRepository;
    private final SpotDailyDemandQueryRepository spotDailyDemandQueryRepository;

    public Course confirmCourse(Long courseId, Long memberId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("존재하지 않는 코스입니다."));

        if (!roomParticipantQueryRepository.isHost(course.getTravelRoomId(), memberId)) {
            throw new CourseAccessDeniedException("방장만 코스를 확정할 수 있습니다.");
        }

        if (course.getStatus() == CourseStatus.CONFIRMED) {
            return course;
        }

        course.confirm();
        travelRoomStatusQueryRepository.markCourseConfirmed(course.getTravelRoomId());

        long participantCount = roomParticipantQueryRepository.countParticipants(course.getTravelRoomId());
        for (CourseSpot courseSpot : courseSpotRepository.findByCourseIdOrderByVisitOrderAsc(courseId)) {
            if (courseSpot.getSpotRole() == SpotRole.ATTRACTION) {
                spotDailyDemandQueryRepository.increment(courseSpot.getTouristSpotId(), course.getTravelDate(),
                        (int) participantCount);
            }
        }

        return course;
    }
}
