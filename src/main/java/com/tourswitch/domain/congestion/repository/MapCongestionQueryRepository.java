package com.tourswitch.domain.congestion.repository;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MapCongestionQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<MapCongestionRow> findLatestAreas() {
        return jdbcTemplate.query("""
                SELECT area.id AS area_id, area.area_code, area.area_name, area.category,
                       area.latitude AS area_latitude, area.longitude AS area_longitude,
                       population.congestion_level, population.congestion_message,
                       population.population_min, population.population_max,
                       population.observed_at, population.collected_at,
                       tourist_spot.content_id, tourist_spot.title AS tourist_spot_title,
                       tourist_spot.first_image_url, tourist_spot.latitude AS tourist_spot_latitude,
                       tourist_spot.longitude AS tourist_spot_longitude
                FROM seoul_realtime_area area
                LEFT JOIN seoul_realtime_population population
                  ON population.id = (
                    SELECT latest_population.id
                    FROM seoul_realtime_population latest_population
                    WHERE latest_population.seoul_realtime_area_id = area.id
                    ORDER BY latest_population.observed_at DESC, latest_population.id DESC
                    LIMIT 1
                  )
                LEFT JOIN spot_area_link area_link
                  ON area_link.seoul_realtime_area_id = area.id AND area_link.is_primary = TRUE
                LEFT JOIN tourist_spot tourist_spot
                  ON tourist_spot.id = area_link.tourist_spot_id AND tourist_spot.is_active = TRUE
                ORDER BY area.area_name, tourist_spot.title
                """, (resultSet, rowNumber) -> new MapCongestionRow(
                resultSet.getLong("area_id"),
                resultSet.getString("area_code"),
                resultSet.getString("area_name"),
                resultSet.getString("category"),
                resultSet.getBigDecimal("area_latitude"),
                resultSet.getBigDecimal("area_longitude"),
                resultSet.getString("congestion_level"),
                resultSet.getString("congestion_message"),
                nullableInteger(resultSet.getObject("population_min")),
                nullableInteger(resultSet.getObject("population_max")),
                resultSet.getObject("observed_at", LocalDateTime.class),
                resultSet.getObject("collected_at", LocalDateTime.class),
                resultSet.getString("content_id"),
                resultSet.getString("tourist_spot_title"),
                resultSet.getString("first_image_url"),
                resultSet.getBigDecimal("tourist_spot_latitude"),
                resultSet.getBigDecimal("tourist_spot_longitude")
        ));
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    public record MapCongestionRow(
            Long areaId,
            String areaCode,
            String areaName,
            String category,
            java.math.BigDecimal areaLatitude,
            java.math.BigDecimal areaLongitude,
            String congestionLevel,
            String congestionMessage,
            Integer populationMin,
            Integer populationMax,
            LocalDateTime observedAt,
            LocalDateTime collectedAt,
            String contentId,
            String touristSpotTitle,
            String firstImageUrl,
            java.math.BigDecimal touristSpotLatitude,
            java.math.BigDecimal touristSpotLongitude
    ) {
    }
}
