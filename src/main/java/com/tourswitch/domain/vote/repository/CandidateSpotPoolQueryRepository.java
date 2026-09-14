package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CandidateSpotPoolQueryRepository {

    private static final List<Integer> CANDIDATE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<CandidateSpotRow> findCandidatePool(Long regionId, List<Long> keywordIds, LocalDate travelDate) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT tourist_spot.content_id, tourist_spot.title, tourist_spot.first_image_url,
                       tourist_spot.latitude, tourist_spot.longitude, keyword_link.keyword_id,
                       forecast.concentration_rate, forecast.concentration_grade
                FROM tourist_spot tourist_spot
                JOIN spot_keyword_link keyword_link
                  ON keyword_link.tourist_spot_id = tourist_spot.id
                LEFT JOIN spot_crowd_link crowd_link
                  ON crowd_link.tourist_spot_id = tourist_spot.id
                LEFT JOIN spot_crowd_forecast forecast
                  ON forecast.attraction_name = crowd_link.attraction_name
                 AND forecast.district_code = crowd_link.district_code
                 AND forecast.forecast_date = :travelDate
                WHERE tourist_spot.region_id = :regionId
                  AND tourist_spot.is_active = TRUE
                  AND tourist_spot.is_coordinate_valid = TRUE
                  AND tourist_spot.content_type_id IN (:contentTypeIds)
                  AND keyword_link.keyword_id IN (:keywordIds)
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                ORDER BY keyword_link.keyword_id,
                         CASE WHEN forecast.concentration_rate IS NULL THEN 1 ELSE 0 END,
                         forecast.concentration_rate, tourist_spot.id
                """)
                .setParameter("regionId", regionId)
                .setParameter("keywordIds", keywordIds)
                .setParameter("travelDate", travelDate)
                .setParameter("contentTypeIds", CANDIDATE_CONTENT_TYPE_IDS)
                .getResultList();

        return rows.stream().map(this::toRow).toList();
    }

    private CandidateSpotRow toRow(Object[] row) {
        return new CandidateSpotRow(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).doubleValue(),
                ((Number) row[4]).doubleValue(),
                ((Number) row[5]).longValue(),
                (BigDecimal) row[6],
                (String) row[7]
        );
    }
}
