package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * tourist_spot/spot_keyword_link/spot_crowd_link/spot_crowd_forecast는 이 도메인이 소유하지 않는
 * 테이블이라 JPA 엔티티로 매핑하지 않고 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * spot_keyword_link는 매칭 배치가 아직 실행되지 않아 현재는 빈 결과를 반환한다.
 */
@Repository
public class CandidateSpotPoolQueryRepository {

    private static final List<Integer> CANDIDATE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<CandidateSpotRow> findCandidatePool(Long regionId, List<Long> keywordIds, LocalDate travelDate) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT ts.id, skl.keyword_id, scf.concentration_percentile, scf.concentration_rate, scf.concentration_grade
                FROM tourist_spot ts
                JOIN spot_keyword_link skl ON skl.tourist_spot_id = ts.id
                LEFT JOIN spot_crowd_link scl ON scl.tourist_spot_id = ts.id
                LEFT JOIN spot_crowd_forecast scf ON scf.district_code = scl.district_code
                    AND scf.attraction_name = scl.attraction_name
                    AND scf.forecast_date = :travelDate
                WHERE ts.region_id = :regionId
                  AND ts.is_active = TRUE
                  AND ts.content_type_id IN (:contentTypeIds)
                  AND skl.keyword_id IN (:keywordIds)
                ORDER BY skl.keyword_id ASC
                """)
                .setParameter("regionId", regionId)
                .setParameter("keywordIds", keywordIds)
                .setParameter("travelDate", travelDate)
                .setParameter("contentTypeIds", CANDIDATE_CONTENT_TYPE_IDS)
                .getResultList();

        return rows.stream()
                .map(row -> new CandidateSpotRow(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2],
                        (BigDecimal) row[3],
                        (String) row[4]))
                .toList();
    }
}
