package com.tourswitch.domain.data.scheduler;

import com.tourswitch.domain.data.service.DerivedDataSyncService;
import com.tourswitch.domain.data.service.DataSyncVerificationService;
import com.tourswitch.domain.data.service.ExternalDataSyncService;
import com.tourswitch.global.config.DataSyncProperties;
import java.util.function.IntSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalDataSyncScheduler {

    private final ExternalDataSyncService service;
    private final DerivedDataSyncService derivedDataSyncService;
    private final DataSyncVerificationService verificationService;
    private final DataSyncProperties properties;

    @Scheduled(cron = "${data-sync.tourist-spots-cron}")
    public void syncTouristSpots() {
        if (properties.enabled()) {
            run("tourist_spot", service::syncTouristSpots);
        }
    }

    @Scheduled(cron = "${data-sync.tourist-details-cron}")
    public void syncTouristDetails() {
        if (properties.enabled()) {
            run("tourist_spot.overview/spot_accessibility", service::syncTouristDetails);
        }
    }

    @Scheduled(cron = "${data-sync.crowd-forecasts-cron}")
    public void syncCrowdForecasts() {
        if (properties.enabled()) {
            run("spot_crowd_forecast", service::syncCrowdForecasts);
        }
    }

    @Scheduled(cron = "${data-sync.seoul-realtime-cron}")
    public void syncSeoulRealtime() {
        if (properties.enabled()) {
            run("seoul_realtime", service::syncSeoulRealtime);
        }
    }

    @Scheduled(cron = "${data-sync.realtime-statistics-cron}")
    public void syncRealtimeStatistics() {
        if (properties.enabled()) {
            run("seoul_realtime_area.reference_population_max",
                    derivedDataSyncService::synchronizeReferencePopulationMaximums);
        }
    }

    @Scheduled(cron = "${data-sync.integrity-check-cron}")
    public void checkDataIntegrity() {
        if (!properties.enabled()) {
            return;
        }
        int issueCount = verificationService.countIntegrityIssues();
        if (issueCount > 0) {
            log.warn("데이터 적재 무결성 점검에서 {}건의 문제가 발견되었습니다.", issueCount);
        } else {
            log.info("데이터 적재 무결성 점검 완료: 문제 없음");
        }
    }

    private void run(String name, IntSupplier task) {
        try {
            log.info("{} 적재 완료: {}건", name, task.getAsInt());
        } catch (Exception e) {
            log.error("{} 적재 실패", name, e);
        }
    }
}
