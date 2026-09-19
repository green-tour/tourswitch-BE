package com.tourswitch.domain.realtimechange.service;

import com.tourswitch.global.client.seoul.SeoulCityDataClient;
import com.tourswitch.global.client.seoul.SeoulCrowdSnapshot;
import com.tourswitch.global.client.seoul.SeoulOpenApiProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class SeoulCrowdCollectionService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private final SeoulCityDataClient client;
    private final SeoulOpenApiProperties properties;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public SeoulCrowdCollectionService(
            SeoulCityDataClient client,
            SeoulOpenApiProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.client = client;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 영역마다 별도 트랜잭션으로 저장한다.
     * 수집 전체를 한 트랜잭션으로 묶으면 한 영역의 제약 위반이 트랜잭션을 rollback-only로 표시해
     * 예외를 잡아도 커밋 시점에 나머지 영역의 정상 데이터까지 함께 롤백된다.
     */
    @Scheduled(initialDelayString = "${seoul-open-api.collection-initial-delay-ms:10000}",
            fixedDelayString = "${seoul-open-api.collection-delay-ms:600000}")
    public void collect() {
        if (!properties.collectionEnabled()) return;
        for (Object[] area : findAreas()) {
            Long areaId = ((Number) area[0]).longValue();
            String areaCode = (String) area[1];
            try {
                client.getCrowd(areaCode).ifPresent(snapshot ->
                        transactionTemplate.executeWithoutResult(status -> save(areaId, snapshot)));
            } catch (RuntimeException exception) {
                log.warn("서울시 혼잡도 수집 실패. areaCode={}", areaCode, exception);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findAreas() {
        return entityManager.createNativeQuery("""
                SELECT id, area_code FROM seoul_realtime_area
                WHERE area_code IS NOT NULL ORDER BY id
                """).getResultList();
    }

    private void save(Long areaId, SeoulCrowdSnapshot snapshot) {
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO seoul_realtime_population
                    (seoul_realtime_area_id, congestion_level, congestion_message,
                     population_min, population_max, observed_at, collected_at)
                SELECT :areaId, :level, :message, :populationMin, :populationMax, :observedAt, :collectedAt
                WHERE NOT EXISTS (
                    SELECT 1 FROM seoul_realtime_population
                    WHERE seoul_realtime_area_id = :areaId AND observed_at = :observedAt
                )
                """)
                .setParameter("areaId", areaId)
                .setParameter("level", snapshot.congestionLevel())
                .setParameter("message", snapshot.congestionMessage())
                .setParameter("populationMin", snapshot.populationMin())
                .setParameter("populationMax", snapshot.populationMax())
                .setParameter("observedAt", snapshot.observedAt())
                .setParameter("collectedAt", LocalDateTime.now(SEOUL_ZONE_ID))
                .executeUpdate();
        if (inserted > 0) log.debug("서울시 혼잡도 저장 완료. areaCode={}", snapshot.areaCode());
    }
}
