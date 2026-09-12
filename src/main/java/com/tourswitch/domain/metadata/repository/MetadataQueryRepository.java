package com.tourswitch.domain.metadata.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MetadataQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<RegionMetadataRow> findRegions() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, district_name
                FROM region
                ORDER BY district_code ASC, id ASC
                """).getResultList();
        return rows.stream().map(row -> new RegionMetadataRow(number(row[0]), (String) row[1])).toList();
    }

    @SuppressWarnings("unchecked")
    public List<KeywordMetadataRow> findActiveKeywords() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, keyword_name
                FROM keyword
                WHERE is_active = TRUE
                ORDER BY display_order ASC, id ASC
                """).getResultList();
        return rows.stream().map(row -> new KeywordMetadataRow(number(row[0]), (String) row[1])).toList();
    }

    private static Long number(Object value) {
        return ((Number) value).longValue();
    }
}
