package com.tourswitch.domain.place.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceQueryRepository {

    private static final List<Integer> SEARCHABLE_CONTENT_TYPES = List.of(12, 14, 15, 28);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public boolean regionExists(Long regionId) {
        if (regionId == null) {
            return true;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM region WHERE id = :regionId",
                Map.of("regionId", regionId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public List<PlaceSummaryRow> findPlaces(Long regionId, List<String> classificationCodes,
                                            LocalDate targetDate, int offset, int size) {
        Query query = searchQuery(regionId, classificationCodes);
        query.parameters().put("targetDate", targetDate);
        query.parameters().put("offset", offset);
        query.parameters().put("size", size);
        return jdbcTemplate.query("""
                SELECT tourist_spot.content_id, tourist_spot.title, region.district_name,
                       tourist_spot.first_image_url, forecast.concentration_grade,
                       forecast.concentration_rate
                FROM tourist_spot tourist_spot
                LEFT JOIN region region ON region.id = tourist_spot.region_id
                LEFT JOIN spot_crowd_link crowd_link ON crowd_link.tourist_spot_id = tourist_spot.id
                LEFT JOIN spot_crowd_forecast forecast
                  ON forecast.attraction_name = crowd_link.attraction_name
                 AND forecast.district_code = crowd_link.district_code
                 AND forecast.forecast_date = :targetDate
                """ + query.whereClause() + """
                ORDER BY CASE WHEN forecast.concentration_rate IS NULL THEN 1 ELSE 0 END,
                         forecast.concentration_rate, tourist_spot.title, tourist_spot.id
                LIMIT :size OFFSET :offset
                """, query.parameters(), (resultSet, rowNumber) -> new PlaceSummaryRow(
                resultSet.getString("content_id"),
                resultSet.getString("title"),
                resultSet.getString("district_name"),
                resultSet.getString("first_image_url"),
                resultSet.getString("concentration_grade"),
                resultSet.getBigDecimal("concentration_rate")
        ));
    }

    public long countPlaces(Long regionId, List<String> classificationCodes) {
        Query query = searchQuery(regionId, classificationCodes);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tourist_spot tourist_spot " + query.whereClause(),
                query.parameters(),
                Long.class
        );
        return count == null ? 0 : count;
    }

    public Optional<PlaceDetailRow> findPlace(String contentId, LocalDate targetDate) {
        List<PlaceDetailRow> rows = jdbcTemplate.query("""
                SELECT tourist_spot.content_id, tourist_spot.title, region.district_name,
                       tourist_spot.overview, tourist_spot.first_image_url, tourist_spot.address,
                       tourist_spot.latitude, tourist_spot.longitude, tourist_spot.data_synced_at,
                       accessibility.has_wheelchair_access, accessibility.has_stroller_access,
                       accessibility.wheelchair_description, accessibility.stroller_description,
                       forecast.concentration_grade, forecast.concentration_rate,
                       forecast.collected_at AS forecast_collected_at,
                       population.congestion_level, population.congestion_message,
                       population.population_min, population.population_max,
                       population.observed_at, population.collected_at AS realtime_collected_at
                FROM tourist_spot tourist_spot
                LEFT JOIN region region ON region.id = tourist_spot.region_id
                LEFT JOIN spot_accessibility accessibility
                  ON accessibility.tourist_spot_id = tourist_spot.id
                LEFT JOIN spot_crowd_link crowd_link
                  ON crowd_link.tourist_spot_id = tourist_spot.id
                LEFT JOIN spot_crowd_forecast forecast
                  ON forecast.attraction_name = crowd_link.attraction_name
                 AND forecast.district_code = crowd_link.district_code
                 AND forecast.forecast_date = :targetDate
                LEFT JOIN spot_area_link area_link
                  ON area_link.tourist_spot_id = tourist_spot.id AND area_link.is_primary = TRUE
                LEFT JOIN seoul_realtime_population population
                  ON population.id = (
                    SELECT latest_population.id
                    FROM seoul_realtime_population latest_population
                    WHERE latest_population.seoul_realtime_area_id = area_link.seoul_realtime_area_id
                    ORDER BY latest_population.observed_at DESC, latest_population.id DESC
                    LIMIT 1
                  )
                WHERE tourist_spot.content_id = :contentId AND tourist_spot.is_active = TRUE
                """, Map.of("contentId", contentId, "targetDate", targetDate),
                (resultSet, rowNumber) -> new PlaceDetailRow(
                        resultSet.getString("content_id"),
                        resultSet.getString("title"),
                        resultSet.getString("district_name"),
                        resultSet.getString("overview"),
                        resultSet.getString("first_image_url"),
                        resultSet.getString("address"),
                        resultSet.getDouble("latitude"),
                        resultSet.getDouble("longitude"),
                        resultSet.getBoolean("has_wheelchair_access"),
                        resultSet.getBoolean("has_stroller_access"),
                        resultSet.getString("wheelchair_description"),
                        resultSet.getString("stroller_description"),
                        resultSet.getString("concentration_grade"),
                        resultSet.getBigDecimal("concentration_rate"),
                        resultSet.getObject("forecast_collected_at", LocalDateTime.class),
                        resultSet.getString("congestion_level"),
                        resultSet.getString("congestion_message"),
                        nullableInteger(resultSet.getObject("population_min")),
                        nullableInteger(resultSet.getObject("population_max")),
                        resultSet.getObject("observed_at", LocalDateTime.class),
                        resultSet.getObject("realtime_collected_at", LocalDateTime.class),
                        resultSet.getObject("data_synced_at", LocalDateTime.class)
                ));
        return rows.stream().findFirst();
    }

    public List<PlaceForecastRow> findForecasts(String contentId, LocalDate fromDate, LocalDate toDate) {
        return jdbcTemplate.query("""
                SELECT forecast.forecast_date, forecast.concentration_rate,
                       forecast.concentration_grade, forecast.collected_at
                FROM tourist_spot tourist_spot
                JOIN spot_crowd_link crowd_link ON crowd_link.tourist_spot_id = tourist_spot.id
                JOIN spot_crowd_forecast forecast
                  ON forecast.attraction_name = crowd_link.attraction_name
                 AND forecast.district_code = crowd_link.district_code
                WHERE tourist_spot.content_id = :contentId
                  AND forecast.forecast_date BETWEEN :fromDate AND :toDate
                ORDER BY forecast.forecast_date
                """, Map.of("contentId", contentId, "fromDate", fromDate, "toDate", toDate),
                (resultSet, rowNumber) -> new PlaceForecastRow(
                        resultSet.getObject("forecast_date", LocalDate.class),
                        resultSet.getBigDecimal("concentration_rate"),
                        resultSet.getString("concentration_grade"),
                        resultSet.getObject("collected_at", LocalDateTime.class)
                ));
    }

    private Query searchQuery(Long regionId, List<String> classificationCodes) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("contentTypes", SEARCHABLE_CONTENT_TYPES);
        StringBuilder whereClause = new StringBuilder("""
                WHERE tourist_spot.is_active = TRUE
                  AND tourist_spot.is_coordinate_valid = TRUE
                  AND tourist_spot.content_type_id IN (:contentTypes)
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                """);
        if (regionId != null) {
            whereClause.append(" AND tourist_spot.region_id = :regionId\n");
            parameters.put("regionId", regionId);
        }
        if (!classificationCodes.isEmpty()) {
            whereClause.append(" AND tourist_spot.classification_level2_code IN (:classificationCodes)\n");
            parameters.put("classificationCodes", classificationCodes);
        }
        return new Query(whereClause.toString(), parameters);
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private record Query(String whereClause, Map<String, Object> parameters) {
    }

    public record PlaceSummaryRow(String contentId, String title, String regionName, String imageUrl,
                                  String congestionGrade, BigDecimal concentrationRate) {
    }

    public record PlaceDetailRow(
            String contentId, String title, String regionName, String overview, String imageUrl,
            String address, double latitude, double longitude,
            boolean wheelchairAccessible, boolean strollerAccessible,
            String wheelchairDescription, String strollerDescription,
            String forecastGrade, BigDecimal forecastRate, LocalDateTime forecastCollectedAt,
            String realtimeLevel, String realtimeMessage, Integer populationMin, Integer populationMax,
            LocalDateTime observedAt, LocalDateTime realtimeCollectedAt, LocalDateTime dataSyncedAt
    ) {
    }

    public record PlaceForecastRow(LocalDate forecastDate, BigDecimal concentrationRate,
                                   String concentrationGrade, LocalDateTime collectedAt) {
    }
}
