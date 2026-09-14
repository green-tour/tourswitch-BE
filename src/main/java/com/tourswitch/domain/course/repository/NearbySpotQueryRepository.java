package com.tourswitch.domain.course.repository;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NearbySpotQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<NearbySpotRow> findNearby(double anchorLatitude, double anchorLongitude, int contentTypeId,
                                           double radiusMeters, int limit) {
        return jdbcTemplate.query("""
                SELECT tourist_spot.content_id,
                       ROUND(ST_Distance_Sphere(
                           tourist_spot.location_point,
                           ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                               4326, 'axis-order=long-lat')
                       )) AS distance_meters
                FROM tourist_spot tourist_spot
                WHERE tourist_spot.content_type_id = :contentTypeId
                  AND tourist_spot.is_active = TRUE
                  AND tourist_spot.is_coordinate_valid = TRUE
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                  AND ST_Distance_Sphere(
                        tourist_spot.location_point,
                        ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                            4326, 'axis-order=long-lat')
                      ) <= :radiusMeters
                ORDER BY distance_meters, tourist_spot.id
                LIMIT :limit
                """, Map.of(
                "latitude", anchorLatitude,
                "longitude", anchorLongitude,
                "contentTypeId", contentTypeId,
                "radiusMeters", radiusMeters,
                "limit", limit
        ), (resultSet, rowNumber) -> new NearbySpotRow(
                resultSet.getString("content_id"),
                resultSet.getInt("distance_meters")
        ));
    }
}
