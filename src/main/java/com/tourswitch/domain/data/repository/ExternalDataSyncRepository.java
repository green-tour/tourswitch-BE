package com.tourswitch.domain.data.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.domain.data.service.TourApiClient.AccessibilitySource;
import com.tourswitch.domain.data.service.TourApiClient.CrowdForecastSource;
import com.tourswitch.domain.data.service.TourApiClient.FestivalPeriodSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristOverviewSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristSpotSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExternalDataSyncRepository {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void seedRegions(List<RegionSeed> regions) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO region
                  (area_code, area_name, district_code, district_name,
                   legal_dong_area_code, legal_dong_district_code)
                VALUES ('11', '서울특별시', ?, ?, '11', ?)
                ON DUPLICATE KEY UPDATE
                  area_name = VALUES(area_name),
                  district_name = VALUES(district_name),
                  legal_dong_area_code = VALUES(legal_dong_area_code),
                  legal_dong_district_code = VALUES(legal_dong_district_code)
                """, regions, BATCH_SIZE, (statement, region) -> {
            statement.setString(1, region.districtCode());
            statement.setString(2, region.districtName());
            statement.setString(3, region.districtCode());
        });
    }

    public void replaceTouristSpots(List<TouristSpotUpsertCommand> commands) {
        jdbcTemplate.update("""
                UPDATE tourist_spot tourist_spot
                LEFT JOIN region region ON region.id = tourist_spot.region_id
                SET tourist_spot.is_active = FALSE
                WHERE region.area_code = '11' OR tourist_spot.region_id IS NULL
                """);
        jdbcTemplate.batchUpdate("""
                INSERT INTO tourist_spot
                  (content_id, content_type_id, title, normalized_title, address, latitude, longitude,
                   location_point, first_image_url, classification_level1_code, classification_level2_code,
                   classification_level3_code, region_id, is_coordinate_valid, has_crowd_data, is_active,
                   data_synced_at)
                VALUES (?, ?, ?, ?, ?, ?, ?,
                        ST_GeomFromText(CONCAT('POINT(', ?, ' ', ?, ')'), 4326, 'axis-order=long-lat'),
                        ?, ?, ?, ?,
                        (SELECT id
                         FROM region
                         WHERE area_code = '11'
                           AND (district_code = ? OR ? LIKE CONCAT('%', district_name, '%'))
                         ORDER BY CASE WHEN district_code = ? THEN 0 ELSE 1 END
                         LIMIT 1),
                        ?, FALSE, TRUE, UTC_TIMESTAMP())
                ON DUPLICATE KEY UPDATE
                  content_type_id = VALUES(content_type_id), title = VALUES(title),
                  normalized_title = VALUES(normalized_title), address = VALUES(address),
                  latitude = VALUES(latitude), longitude = VALUES(longitude),
                  location_point = VALUES(location_point), first_image_url = VALUES(first_image_url),
                  classification_level1_code = VALUES(classification_level1_code),
                  classification_level2_code = VALUES(classification_level2_code),
                  classification_level3_code = VALUES(classification_level3_code),
                  region_id = VALUES(region_id), is_coordinate_valid = VALUES(is_coordinate_valid),
                  is_active = TRUE, data_synced_at = UTC_TIMESTAMP()
                """, commands, BATCH_SIZE, this::setTouristSpotParameters);
    }

    public void replaceFestivalPeriods(List<FestivalPeriodSource> festivals) {
        jdbcTemplate.update("""
                UPDATE tourist_spot
                SET event_start_date = NULL, event_end_date = NULL
                WHERE content_type_id = 15 AND is_active = TRUE
                """);
        jdbcTemplate.batchUpdate("""
                UPDATE tourist_spot
                SET event_start_date = ?, event_end_date = ?
                WHERE content_id = ? AND content_type_id = 15
                """, festivals, BATCH_SIZE, (statement, festival) -> {
            statement.setObject(1, festival.startDate());
            statement.setObject(2, festival.endDate());
            statement.setString(3, festival.contentId());
        });
    }

    public List<String> findActiveCardContentIds() {
        return jdbcTemplate.queryForList("""
                SELECT content_id
                FROM tourist_spot
                WHERE is_active = TRUE AND content_type_id IN (12, 14, 15, 28)
                ORDER BY id
                """, String.class);
    }

    public List<String> findActiveCardContentIdsWithoutOverview() {
        return jdbcTemplate.queryForList("""
                SELECT content_id
                FROM tourist_spot
                WHERE is_active = TRUE
                  AND content_type_id IN (12, 14, 15, 28)
                  AND overview IS NULL
                ORDER BY id
                """, String.class);
    }

    public void updateOverviews(List<TouristOverviewSource> overviews) {
        jdbcTemplate.batchUpdate("""
                UPDATE tourist_spot SET overview = ?, data_synced_at = UTC_TIMESTAMP() WHERE content_id = ?
                """, overviews, BATCH_SIZE, (statement, overview) -> {
            statement.setString(1, overview.overview());
            statement.setString(2, overview.contentId());
        });
    }

    public void upsertCrowdForecasts(List<CrowdForecastUpsertCommand> commands) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO spot_crowd_forecast
                  (area_code, district_code, district_name, attraction_name, normalized_attraction_name,
                   forecast_date, concentration_rate, collected_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())
                ON DUPLICATE KEY UPDATE
                  district_name = VALUES(district_name),
                  normalized_attraction_name = VALUES(normalized_attraction_name),
                  concentration_rate = VALUES(concentration_rate),
                  collected_at = UTC_TIMESTAMP()
                """, commands, BATCH_SIZE, (statement, command) -> {
            CrowdForecastSource source = command.source();
            statement.setString(1, source.areaCode());
            statement.setString(2, source.districtCode());
            statement.setString(3, source.districtName());
            statement.setString(4, source.attractionName());
            statement.setString(5, command.normalizedAttractionName());
            statement.setObject(6, source.forecastDate());
            statement.setBigDecimal(7, source.concentrationRate());
        });
    }

    public void deleteExpiredCrowdForecasts() {
        jdbcTemplate.update("DELETE FROM spot_crowd_forecast WHERE forecast_date < UTC_DATE()");
    }

    public void replaceAccessibility(List<AccessibilityUpsertCommand> commands) {
        jdbcTemplate.update("DELETE FROM spot_accessibility");
        jdbcTemplate.batchUpdate("""
                INSERT INTO spot_accessibility
                  (tourist_spot_id, has_wheelchair_access, has_stroller_access,
                   wheelchair_description, stroller_description, barrier_free_detail, synced_at)
                SELECT id, ?, ?, ?, ?, CAST(? AS JSON), UTC_TIMESTAMP()
                FROM tourist_spot WHERE content_id = ?
                ON DUPLICATE KEY UPDATE
                  has_wheelchair_access = VALUES(has_wheelchair_access),
                  has_stroller_access = VALUES(has_stroller_access),
                  wheelchair_description = VALUES(wheelchair_description),
                  stroller_description = VALUES(stroller_description),
                  barrier_free_detail = VALUES(barrier_free_detail), synced_at = UTC_TIMESTAMP()
                """, commands, BATCH_SIZE, this::setAccessibilityParameters);
    }

    public DataSyncMetrics findDataSyncMetrics() {
        return new DataSyncMetrics(
                count("SELECT COUNT(*) FROM region WHERE area_code = '11'"),
                count("SELECT COUNT(*) FROM keyword WHERE is_active = TRUE"),
                count("SELECT COUNT(*) FROM keyword_classification"),
                count("SELECT COUNT(*) FROM tourist_spot WHERE is_active = TRUE"),
                count("SELECT COUNT(*) FROM tourist_spot WHERE is_active = TRUE AND region_id IS NULL"),
                count("""
                        SELECT COUNT(*) FROM tourist_spot
                        WHERE is_active = TRUE AND classification_level2_code IS NULL
                        """),
                count("""
                        SELECT COUNT(*) FROM tourist_spot
                        WHERE is_active = TRUE AND content_type_id IN (12, 14, 15, 28) AND overview IS NULL
                        """),
                count("SELECT COUNT(*) FROM spot_accessibility"),
                count("SELECT COUNT(*) FROM spot_keyword_link"),
                count("SELECT COUNT(*) FROM spot_crowd_forecast WHERE forecast_date >= UTC_DATE()"),
                count("SELECT COUNT(*) FROM spot_crowd_link"),
                count("""
                        SELECT COUNT(*)
                        FROM spot_crowd_link crowd_link
                        WHERE NOT EXISTS (
                          SELECT 1 FROM spot_crowd_forecast forecast
                          WHERE forecast.attraction_name = crowd_link.attraction_name
                            AND forecast.district_code = crowd_link.district_code
                            AND forecast.forecast_date >= UTC_DATE()
                        )
                        """),
                count("SELECT COUNT(*) FROM crowd_grade_threshold WHERE forecast_date >= UTC_DATE()"),
                count("SELECT COUNT(*) FROM seoul_realtime_area"),
                count("SELECT COUNT(*) FROM seoul_realtime_area WHERE ST_IsValid(boundary) = FALSE"),
                count("""
                        SELECT COUNT(DISTINCT seoul_realtime_area_id)
                        FROM seoul_realtime_population
                        WHERE collected_at >= UTC_TIMESTAMP() - INTERVAL 1 HOUR
                        """),
                count("SELECT COUNT(*) FROM spot_area_link"),
                count("SELECT COUNT(*) FROM spot_duplicate_link")
        );
    }

    private void setTouristSpotParameters(
            PreparedStatement statement,
            TouristSpotUpsertCommand command
    ) throws SQLException {
        TouristSpotSource source = command.source();
        statement.setString(1, source.contentId());
        statement.setInt(2, source.contentTypeId());
        statement.setString(3, source.title());
        statement.setString(4, command.normalizedTitle());
        statement.setString(5, source.address());
        statement.setBigDecimal(6, source.latitude());
        statement.setBigDecimal(7, source.longitude());
        statement.setBigDecimal(8, source.longitude());
        statement.setBigDecimal(9, source.latitude());
        statement.setString(10, source.firstImageUrl());
        statement.setString(11, source.classificationLevel1Code());
        statement.setString(12, source.classificationLevel2Code());
        statement.setString(13, source.classificationLevel3Code());
        statement.setString(14, source.districtCode());
        statement.setString(15, source.address());
        statement.setString(16, source.districtCode());
        statement.setBoolean(17, command.coordinateValid());
    }

    private int count(String sql) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    private void setAccessibilityParameters(
            PreparedStatement statement,
            AccessibilityUpsertCommand command
    ) throws SQLException {
        AccessibilitySource source = command.source();
        statement.setBoolean(1, command.wheelchairAccessible());
        statement.setBoolean(2, command.strollerAccessible());
        statement.setString(3, source.wheelchairDescription());
        statement.setString(4, source.strollerDescription());
        try {
            statement.setString(5, objectMapper.writeValueAsString(source.barrierFreeDetail()));
        } catch (JsonProcessingException exception) {
            throw new SQLException("접근성 상세 JSON 직렬화에 실패했습니다.", exception);
        }
        statement.setString(6, source.contentId());
    }

    public record RegionSeed(String districtCode, String districtName) {
    }

    public record TouristSpotUpsertCommand(
            TouristSpotSource source,
            String normalizedTitle,
            boolean coordinateValid
    ) {
    }

    public record AccessibilityUpsertCommand(
            AccessibilitySource source,
            boolean wheelchairAccessible,
            boolean strollerAccessible
    ) {
    }

    public record CrowdForecastUpsertCommand(
            CrowdForecastSource source,
            String normalizedAttractionName
    ) {
    }

    public record DataSyncMetrics(
            int regions,
            int activeKeywords,
            int keywordClassifications,
            int activeTouristSpots,
            int touristSpotsMissingRegion,
            int touristSpotsMissingClassification,
            int cardSpotsMissingOverview,
            int accessibilityRows,
            int keywordLinks,
            int futureCrowdForecasts,
            int crowdLinks,
            int staleCrowdLinks,
            int futureCrowdGradeThresholds,
            int realtimeAreas,
            int invalidAreaBoundaries,
            int recentlyCollectedRealtimeAreas,
            int areaLinks,
            int duplicateLinks
    ) {
    }
}
