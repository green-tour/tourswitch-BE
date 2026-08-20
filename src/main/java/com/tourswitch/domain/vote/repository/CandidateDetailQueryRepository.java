package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * tourist_spot/keyword/spot_accessibility는 이 도메인이 소유하지 않는 테이블이라
 * 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙). 후보 카드 목록 조회(GET .../candidates)에 쓴다.
 */
@Repository
public class CandidateDetailQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<CandidateDetailRow> findCandidateDetails(Long travelRoomId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT rc.id, rc.tourist_spot_id, rc.keyword_id, k.keyword_name,
                       ts.title, ts.first_image_url, ts.overview, rc.display_order,
                       rc.concentration_rate_snapshot, rc.concentration_grade_snapshot,
                       sa.has_wheelchair_access, sa.has_stroller_access
                FROM room_candidate rc
                JOIN tourist_spot ts ON ts.id = rc.tourist_spot_id
                LEFT JOIN keyword k ON k.id = rc.keyword_id
                LEFT JOIN spot_accessibility sa ON sa.tourist_spot_id = rc.tourist_spot_id
                WHERE rc.travel_room_id = :travelRoomId
                ORDER BY rc.keyword_id ASC, rc.display_order ASC
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getResultList();

        return rows.stream()
                .map(row -> new CandidateDetailRow(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        row[2] == null ? null : ((Number) row[2]).longValue(),
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        ((Number) row[7]).intValue(),
                        (BigDecimal) row[8],
                        (String) row[9],
                        (Boolean) row[10],
                        (Boolean) row[11]))
                .toList();
    }
}
