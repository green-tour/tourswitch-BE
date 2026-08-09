package com.tourswitch.domain.course.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * tourist_spot은 이 도메인이 소유하지 않는 테이블이라 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * course_spot.spot_title_snapshot(확정 당시 명칭)을 채우는 데 쓴다.
 */
@Repository
public class TouristSpotSnapshotQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Map<Long, String> findTitles(List<Long> touristSpotIds) {
        if (touristSpotIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, title FROM tourist_spot WHERE id IN (:ids)
                """)
                .setParameter("ids", touristSpotIds)
                .getResultList();

        return rows.stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> (String) row[1]));
    }
}
