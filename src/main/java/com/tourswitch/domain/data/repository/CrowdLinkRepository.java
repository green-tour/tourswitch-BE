package com.tourswitch.domain.data.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CrowdLinkRepository {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public List<TouristSpotLinkCandidate> findActiveTouristSpots() {
        return jdbcTemplate.query("""
                SELECT ts.id, r.district_code, ts.title
                FROM tourist_spot ts
                JOIN region r ON r.id = ts.region_id
                WHERE ts.is_active = TRUE
                  AND r.district_code IS NOT NULL
                  AND NOT EXISTS (
                    SELECT 1
                    FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = ts.id
                  )
                ORDER BY ts.id
                """, (resultSet, rowNumber) -> new TouristSpotLinkCandidate(
                resultSet.getLong("id"),
                resultSet.getString("district_code"),
                resultSet.getString("title")
        ));
    }

    public List<CrowdForecastLinkCandidate> findCrowdForecastAttractions() {
        return jdbcTemplate.query("""
                SELECT DISTINCT district_code, attraction_name
                FROM spot_crowd_forecast
                WHERE forecast_date >= UTC_DATE()
                ORDER BY district_code, attraction_name
                """, (resultSet, rowNumber) -> new CrowdForecastLinkCandidate(
                resultSet.getString("district_code"),
                resultSet.getString("attraction_name")
        ));
    }

    public void deleteAutomaticallyMatchedLinks() {
        jdbcTemplate.update("""
                DELETE FROM spot_crowd_link
                WHERE match_method IN ('EXACT', 'NORMALIZED')
                """);
    }

    public void saveAll(List<CrowdLinkSaveCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT IGNORE INTO spot_crowd_link
                  (tourist_spot_id, attraction_name, district_code, match_method, is_reviewed, matched_at)
                VALUES (?, ?, ?, ?, FALSE, UTC_TIMESTAMP())
                """, commands, BATCH_SIZE, this::setStatementParameters);
    }

    public void refreshTouristSpotCrowdDataFlags() {
        jdbcTemplate.update("UPDATE tourist_spot SET has_crowd_data = FALSE WHERE has_crowd_data = TRUE");
        jdbcTemplate.update("""
                UPDATE tourist_spot tourist_spot
                JOIN spot_crowd_link crowd_link ON crowd_link.tourist_spot_id = tourist_spot.id
                SET tourist_spot.has_crowd_data = TRUE
                """);
    }

    private void setStatementParameters(
            PreparedStatement statement,
            CrowdLinkSaveCommand command
    ) throws SQLException {
        statement.setLong(1, command.touristSpotId());
        statement.setString(2, command.attractionName());
        statement.setString(3, command.districtCode());
        statement.setString(4, command.matchMethod());
    }
}
