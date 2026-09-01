package com.tourswitch.domain.realtimechange.repository;

import com.tourswitch.domain.realtimechange.entity.CourseReplacement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReplacementRepository extends JpaRepository<CourseReplacement, Long> {

    boolean existsByCourseId(Long courseId);

    Optional<CourseReplacement> findByCourseId(Long courseId);
}
