package com.tourswitch.domain.realtimechange.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RegionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<RegionRow> findAllSeoulDistricts() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, district_code, district_name
                FROM region
                WHERE area_code = '11'
                ORDER BY district_name ASC
                """).getResultList();

        return rows.stream()
                .map(row -> new RegionRow(((Number) row[0]).longValue(), (String) row[1], (String) row[2]))
                .toList();
    }

    public boolean existsById(Long regionId) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM region WHERE id = :regionId")
                .setParameter("regionId", regionId)
                .getSingleResult();
        return count.longValue() > 0;
    }
}
