package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.course.entity.Course;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByTravelRoomId(Long travelRoomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT course FROM Course course WHERE course.id = :courseId")
    Optional<Course> findByIdForUpdate(@Param("courseId") Long courseId);
}
