package com.tourswitch.domain.place.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRegionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<PlaceRegionRow> findById(Long regionId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, district_name, legal_dong_area_code, legal_dong_district_code, district_code
                FROM region
                WHERE id = :regionId
                """)
                .setParameter("regionId", regionId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new PlaceRegionRow(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                (String) row[3], (String) row[4]));
    }

    @SuppressWarnings("unchecked")
    public List<PlaceRegionRow> findAll() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, district_name, legal_dong_area_code, legal_dong_district_code, district_code
                FROM region
                ORDER BY id
                """)
                .getResultList();
        return rows.stream()
                .map(row -> new PlaceRegionRow(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                        (String) row[3], (String) row[4]))
                .toList();
    }
}
