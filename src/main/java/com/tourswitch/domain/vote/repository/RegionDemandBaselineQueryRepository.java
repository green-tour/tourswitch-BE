package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * seoul_realtime_area는 한국관광공사 API가 아닌 서울 열린데이터광장 데이터라 로컬 보관 대상이다
 * (TourAPI 실시간전환 계획 문서 2절). 좌표는 더 이상 로컬 tourist_spot에서 조회하지 않고
 * 호출부(CandidateScoreCalculator)가 TourAPI 응답 좌표를 그대로 넘긴다 - boundary가 이미
 * POLYGON SRID 4326이라 ST_Contains로 즉석 판정한다.
 */
@Repository
public class RegionDemandBaselineQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Integer> findPrimaryReferencePopulationMax(double latitude, double longitude) {
        List<?> results = entityManager.createNativeQuery("""
                SELECT reference_population_max
                FROM seoul_realtime_area
                WHERE ST_Contains(boundary, ST_SRID(POINT(:longitude, :latitude), 4326))
                LIMIT 1
                """)
                .setParameter("longitude", longitude)
                .setParameter("latitude", latitude)
                .getResultList();
        if (results.isEmpty() || results.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(((Number) results.get(0)).intValue());
    }

    private static final int NEARBY_AREA_SAMPLE_SIZE = 5;

    /**
     * 6.2절 규칙 4 폴백: 자치구 규모 대리값 표본. 기존에는 그 자치구에 속한 관광지들이
     * INSIDE_BOUNDARY로 연결된 영역 전체를 표본으로 삼았으나, 로컬 tourist_spot이 없어져
     * 그 경로를 쓸 수 없다 - 대신 region 중심좌표에서 가장 가까운 seoul_realtime_area
     * NEARBY_AREA_SAMPLE_SIZE개를 표본으로 근사한다.
     */
    @SuppressWarnings("unchecked")
    public List<Integer> findRegionInsideAreaPopulations(Long regionId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT sra.id, sra.reference_population_max
                FROM region r
                JOIN seoul_realtime_area sra ON sra.reference_population_max IS NOT NULL
                WHERE r.id = :regionId
                ORDER BY ST_Distance_Sphere(
                    ST_SRID(POINT(sra.longitude, sra.latitude), 4326),
                    ST_SRID(POINT(r.center_longitude, r.center_latitude), 4326)
                ) ASC
                LIMIT :sampleSize
                """)
                .setParameter("regionId", regionId)
                .setParameter("sampleSize", NEARBY_AREA_SAMPLE_SIZE)
                .getResultList();

        List<Integer> populations = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            populations.add(((Number) row[1]).intValue());
        }
        return populations;
    }
}
