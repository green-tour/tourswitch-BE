package com.tourswitch.domain.data.controller;

import com.tourswitch.domain.data.service.ExternalDataSyncService;
import com.tourswitch.domain.data.response.DataSyncResponseDTO;
import com.tourswitch.global.response.GlobalRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영 점검/초기 적재용 수동 실행 API. 정기 실행은 ExternalDataSyncScheduler가 담당한다. */
@Tag(name = "데이터 적재 API")
@RestController
@RequestMapping("/internal/data-sync")
@RequiredArgsConstructor
public class DataSyncController {

    private final ExternalDataSyncService service;

    @Operation(summary = "관광지 데이터 적재")
    @PostMapping("/tourist-spots")
    public GlobalRes<DataSyncResponseDTO> touristSpots() {
        return GlobalRes.success(DataSyncResponseDTO.of(
                "tourist_spot", service.syncTouristSpots()));
    }

    @Operation(summary = "관광지 혼잡도 예측 데이터 적재")
    @PostMapping("/crowd-forecasts")
    public GlobalRes<DataSyncResponseDTO> crowdForecasts() {
        return GlobalRes.success(DataSyncResponseDTO.of(
                "spot_crowd_forecast", service.syncCrowdForecasts()));
    }

    @Operation(summary = "서울 실시간 인구 데이터 적재")
    @PostMapping("/seoul-realtime")
    public GlobalRes<DataSyncResponseDTO> seoulRealtime() {
        return GlobalRes.success(DataSyncResponseDTO.of(
                "seoul_realtime", service.syncSeoulRealtime()));
    }
}
