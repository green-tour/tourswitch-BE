package com.tourswitch.domain.course.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * tourist_spot은 이 도메인이 소유하지 않는 테이블이라 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * MySQL 8.4의 ST_Distance_Sphere로 좌표 간 직선거리(미터)를 구한다 - 실제 도로 경로가 아니라
 * 방문 순서 산정용 근사치이며, 자체 길찾기 엔진은 만들지 않는다(계획 문서 5단계).
 */
@Repository
public class SpotDistanceQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 주어진 관광지들 사이의 대칭 거리 행렬을 만든다. N개 지점이면 N*(N-1)/2쌍만 조회하고
     * 나머지는 대칭으로 채운다.
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Map<Long, Integer>> findDistanceMatrix(List<Long> touristSpotIds) {
        Map<Long, Map<Long, Integer>> matrix = new HashMap<>();
        for (Long spotId : touristSpotIds) {
            matrix.put(spotId, new HashMap<>());
        }
        if (touristSpotIds.size() < 2) {
            return matrix;
        }

        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT a.id, b.id, ST_Distance_Sphere(a.location_point, b.location_point)
                FROM tourist_spot a JOIN tourist_spot b ON a.id < b.id
                WHERE a.id IN (:ids) AND b.id IN (:ids)
                """)
                .setParameter("ids", touristSpotIds)
                .getResultList();

        for (Object[] row : rows) {
            Long a = ((Number) row[0]).longValue();
            Long b = ((Number) row[1]).longValue();
            int distance = ((Number) row[2]).intValue();
            matrix.get(a).put(b, distance);
            matrix.get(b).put(a, distance);
        }
        return matrix;
    }
}
