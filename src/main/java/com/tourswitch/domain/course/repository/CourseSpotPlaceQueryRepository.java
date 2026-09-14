package com.tourswitch.domain.course.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseSpotPlaceQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<CourseSpotPlaceRow> findByCourseId(Long courseId) {
        return jdbcTemplate.query("""
                SELECT course_spot.id AS course_spot_id, tourist_spot.address,
                       tourist_spot.latitude, tourist_spot.longitude
                FROM course_spot course_spot
                LEFT JOIN tourist_spot tourist_spot
                  ON tourist_spot.content_id = course_spot.content_id
                WHERE course_spot.course_id = ?
                ORDER BY course_spot.visit_order
                """, (resultSet, rowNumber) -> new CourseSpotPlaceRow(
                resultSet.getLong("course_spot_id"),
                resultSet.getString("address"),
                nullableDouble(resultSet.getObject("latitude")),
                nullableDouble(resultSet.getObject("longitude"))
        ), courseId);
    }

    private static Double nullableDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    public record CourseSpotPlaceRow(Long courseSpotId, String address, Double latitude, Double longitude) {
    }
}
