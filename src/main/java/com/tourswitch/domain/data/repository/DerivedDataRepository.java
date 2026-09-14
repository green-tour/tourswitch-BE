package com.tourswitch.domain.data.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DerivedDataRepository {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public void upsertKeyword(String keywordName, int displayOrder) {
        jdbcTemplate.update("""
                INSERT INTO keyword (keyword_name, display_order, is_active)
                VALUES (?, ?, TRUE)
                ON DUPLICATE KEY UPDATE display_order = VALUES(display_order), is_active = TRUE
                """, keywordName, displayOrder);
    }

    public void replaceKeywordClassifications(List<KeywordClassification> classifications) {
        jdbcTemplate.update("DELETE FROM keyword_classification");
        jdbcTemplate.batchUpdate("""
                INSERT INTO keyword_classification (keyword_id, classification_level2_code)
                SELECT id, ? FROM keyword WHERE keyword_name = ?
                """, classifications, BATCH_SIZE, (statement, classification) -> {
            statement.setString(1, classification.classificationCode());
            statement.setString(2, classification.keywordName());
        });
    }

    public int replaceSpotKeywordLinks() {
        jdbcTemplate.update("DELETE FROM spot_keyword_link");
        return jdbcTemplate.update("""
                INSERT INTO spot_keyword_link (tourist_spot_id, keyword_id)
                SELECT tourist_spot.id, keyword_classification.keyword_id
                FROM tourist_spot tourist_spot
                JOIN keyword_classification keyword_classification
                  ON keyword_classification.classification_level2_code = tourist_spot.classification_level2_code
                JOIN keyword keyword ON keyword.id = keyword_classification.keyword_id AND keyword.is_active = TRUE
                WHERE tourist_spot.is_active = TRUE
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                """);
    }

    public int upsertConfirmedDuplicate(String duplicateContentId, String canonicalContentId) {
        return jdbcTemplate.update("""
                INSERT INTO spot_duplicate_link
                  (tourist_spot_id, canonical_tourist_spot_id, match_method, is_reviewed, matched_at)
                SELECT duplicate_spot.id, canonical_spot.id, 'MANUAL', TRUE, UTC_TIMESTAMP()
                FROM tourist_spot duplicate_spot
                JOIN tourist_spot canonical_spot ON canonical_spot.content_id = ?
                WHERE duplicate_spot.content_id = ?
                ON DUPLICATE KEY UPDATE
                  canonical_tourist_spot_id = VALUES(canonical_tourist_spot_id),
                  match_method = 'MANUAL', is_reviewed = TRUE, matched_at = UTC_TIMESTAMP()
                """, canonicalContentId, duplicateContentId);
    }

    public List<AreaLinkCandidate> findInsideAreaCandidates() {
        return findAreaCandidates("""
                area.latitude IS NOT NULL
                AND area.longitude IS NOT NULL
                AND ABS(tourist_spot.latitude - area.latitude) <= 0.050
                AND ABS(tourist_spot.longitude - area.longitude) <= 0.070
                AND MBRIntersects(area.boundary, tourist_spot.location_point)
                AND ST_Intersects(area.boundary, tourist_spot.location_point)
                """, true);
    }

    public List<AreaLinkCandidate> findProximityAreaCandidates() {
        return findAreaCandidates("""
                area.latitude IS NOT NULL
                AND area.longitude IS NOT NULL
                AND ABS(tourist_spot.latitude - area.latitude) <= 0.010
                AND ABS(tourist_spot.longitude - area.longitude) <= 0.015
                AND NOT ST_Intersects(area.boundary, tourist_spot.location_point)
                AND ST_Distance_Sphere(
                      tourist_spot.location_point,
                      ST_GeomFromText(CONCAT('POINT(', area.longitude, ' ', area.latitude, ')'),
                          4326, 'axis-order=long-lat')
                    ) <= 1000
                """, false);
    }

    public Set<Long> findManualPrimaryTouristSpotIds() {
        return jdbcTemplate.queryForList("""
                SELECT tourist_spot_id
                FROM spot_area_link
                WHERE match_method = 'MANUAL' AND is_primary = TRUE
                """, Long.class).stream().collect(Collectors.toSet());
    }

    public void deleteAutomaticAreaLinks() {
        jdbcTemplate.update("""
                DELETE FROM spot_area_link
                WHERE match_method IN ('INSIDE_BOUNDARY', 'PROXIMITY')
                """);
    }

    public void saveAreaLinks(List<AreaLinkSaveCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT IGNORE INTO spot_area_link
                  (tourist_spot_id, seoul_realtime_area_id, match_method, distance_meters,
                   name_match_priority, area_size_square_meters, is_primary, is_reviewed, matched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, UTC_TIMESTAMP())
                """, commands, BATCH_SIZE, this::setAreaLinkParameters);
    }

    public List<CrowdGradeCandidate> findCrowdGradeCandidates() {
        return jdbcTemplate.query("""
                SELECT forecast.id, forecast.forecast_date, forecast.concentration_rate
                FROM spot_crowd_forecast forecast
                JOIN spot_crowd_link crowd_link
                  ON crowd_link.attraction_name = forecast.attraction_name
                 AND crowd_link.district_code = forecast.district_code
                JOIN tourist_spot tourist_spot ON tourist_spot.id = crowd_link.tourist_spot_id
                WHERE tourist_spot.is_active = TRUE
                  AND tourist_spot.content_type_id IN (12, 14, 15, 28)
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                ORDER BY forecast.forecast_date, forecast.concentration_rate, forecast.id
                """, (resultSet, rowNumber) -> new CrowdGradeCandidate(
                resultSet.getLong("id"),
                resultSet.getObject("forecast_date", LocalDate.class),
                resultSet.getBigDecimal("concentration_rate")
        ));
    }

    public void clearCrowdGrades() {
        jdbcTemplate.update("""
                UPDATE spot_crowd_forecast
                SET concentration_percentile = NULL, concentration_grade = NULL
                """);
        jdbcTemplate.update("DELETE FROM crowd_grade_threshold");
    }

    public void saveCrowdGrades(List<CrowdGradeUpdate> updates) {
        if (updates.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                UPDATE spot_crowd_forecast
                SET concentration_percentile = ?, concentration_grade = ?
                WHERE id = ?
                """, updates, BATCH_SIZE, (statement, update) -> {
            statement.setBigDecimal(1, update.percentile());
            statement.setString(2, update.grade());
            statement.setLong(3, update.forecastId());
        });
    }

    public void saveCrowdGradeThresholds(List<CrowdGradeThreshold> thresholds) {
        if (thresholds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO crowd_grade_threshold
                  (forecast_date, percentile_25_value, percentile_50_value, percentile_75_value,
                   sample_count, calculated_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP())
                """, thresholds, BATCH_SIZE, (statement, threshold) -> {
            statement.setObject(1, threshold.forecastDate());
            statement.setBigDecimal(2, threshold.percentile25());
            statement.setBigDecimal(3, threshold.percentile50());
            statement.setBigDecimal(4, threshold.percentile75());
            statement.setInt(5, threshold.sampleCount());
        });
    }

    public List<AreaPopulationObservation> findRecentPopulationMaximums() {
        return jdbcTemplate.query("""
                SELECT seoul_realtime_area_id, population_max
                FROM seoul_realtime_population
                WHERE population_max IS NOT NULL
                  AND collected_at >= UTC_TIMESTAMP() - INTERVAL 30 DAY
                ORDER BY seoul_realtime_area_id, population_max
                """, (resultSet, rowNumber) -> new AreaPopulationObservation(
                resultSet.getLong("seoul_realtime_area_id"),
                resultSet.getInt("population_max")
        ));
    }

    public void replaceReferencePopulationMaximums(List<AreaReferencePopulation> references) {
        jdbcTemplate.update("UPDATE seoul_realtime_area SET reference_population_max = NULL");
        if (references.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                UPDATE seoul_realtime_area SET reference_population_max = ? WHERE id = ?
                """, references, BATCH_SIZE, (statement, reference) -> {
            statement.setInt(1, reference.referencePopulationMaximum());
            statement.setLong(2, reference.areaId());
        });
    }

    private List<AreaLinkCandidate> findAreaCandidates(String spatialCondition, boolean insideBoundary) {
        String distanceExpression = insideBoundary
                ? "0"
                : """
                  ROUND(ST_Distance_Sphere(
                      tourist_spot.location_point,
                      ST_GeomFromText(CONCAT('POINT(', area.longitude, ' ', area.latitude, ')'),
                          4326, 'axis-order=long-lat')
                  ))
                  """;
        String sql = """
                SELECT tourist_spot.id AS tourist_spot_id, tourist_spot.title,
                       area.id AS area_id, area.area_code, area.area_name,
                       CAST(ST_Area(area.boundary) AS DECIMAL(14,3)) AS area_size,
                       CAST(%s AS UNSIGNED) AS distance_meters
                FROM tourist_spot tourist_spot
                JOIN seoul_realtime_area area ON (%s)
                WHERE tourist_spot.is_active = TRUE
                  AND tourist_spot.is_coordinate_valid = TRUE
                  AND ST_IsValid(area.boundary) = TRUE
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                ORDER BY tourist_spot.id, area.area_code, area.id
                """.formatted(distanceExpression, spatialCondition);
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new AreaLinkCandidate(
                resultSet.getLong("tourist_spot_id"),
                resultSet.getString("title"),
                resultSet.getLong("area_id"),
                resultSet.getString("area_code"),
                resultSet.getString("area_name"),
                resultSet.getBigDecimal("area_size"),
                resultSet.getInt("distance_meters"),
                insideBoundary
        ));
    }

    private void setAreaLinkParameters(
            PreparedStatement statement,
            AreaLinkSaveCommand command
    ) throws SQLException {
        statement.setLong(1, command.touristSpotId());
        statement.setLong(2, command.areaId());
        statement.setString(3, command.matchMethod());
        statement.setInt(4, command.distanceMeters());
        statement.setInt(5, command.nameMatchPriority());
        statement.setBigDecimal(6, command.areaSizeSquareMeters());
        statement.setBoolean(7, command.primary());
    }

    public record KeywordClassification(String keywordName, String classificationCode) {
    }

    public record AreaLinkCandidate(
            Long touristSpotId,
            String touristSpotTitle,
            Long areaId,
            String areaCode,
            String areaName,
            BigDecimal areaSizeSquareMeters,
            int distanceMeters,
            boolean insideBoundary
    ) {
    }

    public record AreaLinkSaveCommand(
            Long touristSpotId,
            Long areaId,
            String matchMethod,
            int distanceMeters,
            int nameMatchPriority,
            BigDecimal areaSizeSquareMeters,
            boolean primary
    ) {
    }

    public record CrowdGradeCandidate(Long forecastId, LocalDate forecastDate, BigDecimal concentrationRate) {
    }

    public record CrowdGradeUpdate(Long forecastId, BigDecimal percentile, String grade) {
    }

    public record CrowdGradeThreshold(
            LocalDate forecastDate,
            BigDecimal percentile25,
            BigDecimal percentile50,
            BigDecimal percentile75,
            int sampleCount
    ) {
    }

    public record AreaPopulationObservation(Long areaId, int populationMaximum) {
    }

    public record AreaReferencePopulation(Long areaId, int referencePopulationMaximum) {
    }
}
