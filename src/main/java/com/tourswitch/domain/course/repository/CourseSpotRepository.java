package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.course.entity.CourseSpot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSpotRepository extends JpaRepository<CourseSpot, Long> {

    List<CourseSpot> findByCourseIdOrderByVisitOrderAsc(Long courseId);

    Optional<CourseSpot> findByIdAndCourseId(Long id, Long courseId);
}
