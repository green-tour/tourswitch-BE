package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * region은 이 도메인이 소유하지 않는 테이블이라 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * TourAPI가 아닌 우리 자체 지역 기준정보라 로컬 보관 대상이다(TourAPI 실시간전환 계획 문서 2절).
 */
@Repository
public class RegionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<RegionRow> findById(Long regionId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT legal_dong_area_code, legal_dong_district_code, district_code
                FROM region
                WHERE id = :regionId
                """)
                .setParameter("regionId", regionId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new RegionRow((String) row[0], (String) row[1], (String) row[2]));
    }
}
