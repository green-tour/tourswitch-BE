package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * spot_area_link/seoul_realtime_area는 이 도메인이 소유하지 않는 테이블이라
 * JPA 엔티티로 매핑하지 않고 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * 6.2절 규칙 3에 따라 규모 대리값은 INSIDE_BOUNDARY 매칭만 인정한다.
 */
@Repository
public class RegionDemandBaselineQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Integer> findPrimaryReferencePopulationMax(Long touristSpotId) {
        List<?> results = entityManager.createNativeQuery("""
                SELECT sra.reference_population_max
                FROM spot_area_link sal
                JOIN seoul_realtime_area sra ON sra.id = sal.seoul_realtime_area_id
                WHERE sal.tourist_spot_id = :touristSpotId
                  AND sal.is_primary = TRUE
                  AND sal.match_method = 'INSIDE_BOUNDARY'
                """)
                .setParameter("touristSpotId", touristSpotId)
                .getResultList();
        if (results.isEmpty() || results.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(((Number) results.get(0)).intValue());
    }

    /**
     * 6.2절 규칙 4 폴백: 자치구 안에서 INSIDE 연결된 영역들의 규모 대리값 목록(자치구 표본).
     * 영역 단위로 중복 제거한다(같은 영역에 여러 관광지가 연결될 수 있으므로).
     */
    @SuppressWarnings("unchecked")
    public List<Integer> findRegionInsideAreaPopulations(Long regionId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT DISTINCT sra.id, sra.reference_population_max
                FROM spot_area_link sal
                JOIN tourist_spot ts ON ts.id = sal.tourist_spot_id
                JOIN seoul_realtime_area sra ON sra.id = sal.seoul_realtime_area_id
                WHERE ts.region_id = :regionId
                  AND sal.match_method = 'INSIDE_BOUNDARY'
                  AND sra.reference_population_max IS NOT NULL
                """)
                .setParameter("regionId", regionId)
                .getResultList();

        List<Integer> populations = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            populations.add(((Number) row[1]).intValue());
        }
        return populations;
    }
}
