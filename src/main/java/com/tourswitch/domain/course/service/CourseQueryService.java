package com.tourswitch.domain.course.service;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseExtraCandidate;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.exception.CourseNotFoundException;
import com.tourswitch.domain.course.repository.CourseExtraCandidateRepository;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final CourseExtraCandidateRepository courseExtraCandidateRepository;

    public Course getCourseByTravelRoomId(Long travelRoomId) {
        return courseRepository.findByTravelRoomId(travelRoomId)
                .orElseThrow(() -> new CourseNotFoundException("아직 생성된 코스가 없습니다."));
    }

    public List<CourseSpot> getStops(Long courseId) {
        return courseSpotRepository.findByCourseIdOrderByVisitOrderAsc(courseId);
    }

    public List<CourseExtraCandidate> getExtraCandidates(Long courseId) {
        return courseExtraCandidateRepository.findByCourseId(courseId);
    }
}
