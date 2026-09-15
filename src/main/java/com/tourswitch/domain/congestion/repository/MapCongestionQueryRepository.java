package com.tourswitch.domain.congestion.repository;

import com.tourswitch.domain.congestion.model.MapCongestionSnapshot;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서울 121개 영역과 각 영역의 최신 실시간 혼잡도를 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class MapCongestionQueryRepository {

    private final EntityManager entityManager;

    /**
     * 모든 서울 실시간 영역을 최신 혼잡도 한 건과 함께 조회한다.
     */
    @SuppressWarnings("unchecked")
    public List<MapCongestionSnapshot> findLatestAreas() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT area.id, area.area_code, area.area_name, area.category,
                       area.latitude, area.longitude, ST_AsGeoJSON(area.boundary, 7),
                       population.congestion_level, population.congestion_message,
                       population.population_min, population.population_max,
                       population.observed_at, population.collected_at
                FROM seoul_realtime_area area
                LEFT JOIN seoul_realtime_population population
                  ON population.id = (
                    SELECT latest_population.id
                    FROM seoul_realtime_population latest_population
                    WHERE latest_population.seoul_realtime_area_id = area.id
                    ORDER BY latest_population.observed_at DESC, latest_population.id DESC
                    LIMIT 1
                  )
                ORDER BY area.area_name
                """).getResultList();
        return rows.stream().map(this::toSnapshot).toList();
    }

    /**
     * 네이티브 쿼리 결과를 지도 혼잡도 조회 모델로 변환한다.
     */
    private MapCongestionSnapshot toSnapshot(Object[] row) {
        return new MapCongestionSnapshot(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (BigDecimal) row[4],
                (BigDecimal) row[5],
                (String) row[6],
                (String) row[7],
                (String) row[8],
                nullableInteger(row[9]),
                nullableInteger(row[10]),
                (LocalDateTime) row[11],
                (LocalDateTime) row[12]
        );
    }

    /**
     * nullable 숫자 컬럼을 Integer로 안전하게 변환한다.
     */
    private Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
