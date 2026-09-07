package com.tourswitch.domain.data.scheduler;

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
    private final DataSyncProperties properties;

    @Scheduled(cron = "${data-sync.tourist-spots-cron}")
    public void syncTouristSpots() {
        if (properties.enabled()) {
            run("tourist_spot", service::syncTouristSpots);
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

    private void run(String name, IntSupplier task) {
        try {
            log.info("{} 적재 완료: {}건", name, task.getAsInt());
        } catch (Exception e) {
            log.error("{} 적재 실패", name, e);
        }
    }
}
