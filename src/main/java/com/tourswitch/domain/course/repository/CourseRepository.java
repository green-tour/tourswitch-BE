package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.course.entity.Course;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByTravelRoomId(Long travelRoomId);
}
