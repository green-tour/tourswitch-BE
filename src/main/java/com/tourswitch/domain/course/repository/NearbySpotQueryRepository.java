package com.tourswitch.domain.course.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * tourist_spot은 이 도메인이 소유하지 않는 테이블이라 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * 부가 카테고리 후보(DB설계 8.3절): 기준 지점 반경 이내에서 거리순으로 상위 N개를 찾는다.
 */
@Repository
public class NearbySpotQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<NearbySpotRow> findNearby(Long anchorTouristSpotId, int contentTypeId, double radiusMeters,
                                           int limit) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT ts.id, ST_Distance_Sphere(ts.location_point, anchor.location_point) AS distance
                FROM tourist_spot ts, tourist_spot anchor
                WHERE anchor.id = :anchorId
                  AND ts.id != :anchorId
                  AND ts.content_type_id = :contentTypeId
                  AND ts.is_active = TRUE
                  AND ST_Distance_Sphere(ts.location_point, anchor.location_point) <= :radiusMeters
                ORDER BY distance ASC
                """)
                .setParameter("anchorId", anchorTouristSpotId)
                .setParameter("contentTypeId", contentTypeId)
                .setParameter("radiusMeters", radiusMeters)
                .setMaxResults(limit)
                .getResultList();

        return rows.stream()
                .map(row -> new NearbySpotRow(((Number) row[0]).longValue(), ((Number) row[1]).intValue()))
                .toList();
    }
}
