package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.course.entity.CourseExtraCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseExtraCandidateRepository extends JpaRepository<CourseExtraCandidate, Long> {

    List<CourseExtraCandidate> findByCourseId(Long courseId);
}
