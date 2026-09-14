package com.tourswitch.domain.realtimechange.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SeoulRealtimeCrowdQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<SeoulRealtimeCrowdRow> findLatest(double latitude, double longitude) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT population.congestion_level, population.observed_at
                FROM seoul_realtime_area area
                JOIN seoul_realtime_population population
                  ON population.seoul_realtime_area_id = area.id
                WHERE ST_Contains(area.boundary, ST_SRID(POINT(:longitude, :latitude), 4326))
                  AND population.observed_at >= NOW() - INTERVAL 60 MINUTE
                ORDER BY population.observed_at DESC, population.id DESC
                LIMIT 1
                """)
                .setParameter("longitude", longitude)
                .setParameter("latitude", latitude)
                .getResultList();
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(new SeoulRealtimeCrowdRow((String) rows.get(0)[0], (LocalDateTime) rows.get(0)[1]));
    }
}
