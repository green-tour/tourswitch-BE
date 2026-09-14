package com.tourswitch.domain.data.service;

import com.tourswitch.global.config.DataSyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataInitializer implements ApplicationRunner {

    private final ReferenceDataSyncService referenceDataSyncService;
    private final SeoulRealtimeAreaReferenceService seoulRealtimeAreaReferenceService;
    private final DataSyncProperties dataSyncProperties;

    @Override
    public void run(ApplicationArguments arguments) {
        if (!dataSyncProperties.enabled()) {
            return;
        }
        referenceDataSyncService.synchronize();
        int realtimeAreas = seoulRealtimeAreaReferenceService.synchronize();
        log.info("기준정보 초기화 완료: 서울 자치구=25곳, 실시간 영역={}곳", realtimeAreas);
    }
}
