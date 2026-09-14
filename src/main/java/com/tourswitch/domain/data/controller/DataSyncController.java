package com.tourswitch.domain.data.controller;

import com.tourswitch.domain.data.response.DataSyncResponseDTO;
import com.tourswitch.domain.data.response.DataSyncStatusResponseDTO;
import com.tourswitch.domain.data.response.DerivedDataSyncResponseDTO;
import com.tourswitch.domain.data.response.FullDataSyncResponseDTO;
import com.tourswitch.domain.data.service.DataSyncVerificationService;
import com.tourswitch.domain.data.service.DerivedDataSyncService;
import com.tourswitch.domain.data.service.ExternalDataSyncService;
import com.tourswitch.global.response.GlobalRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영 점검/초기 적재용 수동 실행 API. 정기 실행은 ExternalDataSyncScheduler가 담당한다. */
@Tag(name = "데이터 적재 API")
@RestController
@RequestMapping("/internal/data-sync")
@RequiredArgsConstructor
public class DataSyncController {

    private final ExternalDataSyncService service;
    private final DerivedDataSyncService derivedDataSyncService;
    private final DataSyncVerificationService verificationService;

    @Operation(summary = "관광지 데이터 적재")
    @PostMapping("/tourist-spots")
    public GlobalRes<DataSyncResponseDTO> touristSpots() {
        return GlobalRes.success(DataSyncResponseDTO.of(
                "tourist_spot", service.syncTouristSpots()));
    }

    @Operation(summary = "관광지·상세·혼잡도·링크 전체 순차 적재")
    @PostMapping("/all")
    public GlobalRes<FullDataSyncResponseDTO> synchronizeAll() {
        int touristSpots = service.syncTouristSpots();
        int touristDetails = service.syncTouristDetails();
        int crowdForecasts = service.syncCrowdForecasts();
        DerivedDataSyncResponseDTO derivedData = DerivedDataSyncResponseDTO.from(
                derivedDataSyncService.synchronizeAll());
        return GlobalRes.success(new FullDataSyncResponseDTO(
                touristSpots,
                touristDetails,
                crowdForecasts,
                derivedData,
                verificationService.getStatus()
        ));
    }

    @Operation(summary = "관광지 소개 및 접근성 상세 적재")
    @PostMapping("/tourist-details")
    public GlobalRes<DataSyncResponseDTO> touristDetails() {
        return GlobalRes.success(DataSyncResponseDTO.of(
                "tourist_spot.overview/spot_accessibility", service.syncTouristDetails()));
    }

    @Operation(summary = "관광지 혼잡도 예측 데이터 적재")
    @PostMapping("/crowd-forecasts")
    public GlobalRes<DataSyncResponseDTO> crowdForecasts() {
        return GlobalRes.success(DataSyncResponseDTO.of(
                "spot_crowd_forecast", service.syncCrowdForecasts()));
    }

    @Operation(summary = "링크 및 파생 데이터 전체 재생성")
    @PostMapping("/derived-data")
    public GlobalRes<DerivedDataSyncResponseDTO> derivedData() {
        return GlobalRes.success(DerivedDataSyncResponseDTO.from(
                derivedDataSyncService.synchronizeAll()));
    }

    @Operation(summary = "데이터 적재 완전성 점검")
    @GetMapping("/status")
    public GlobalRes<DataSyncStatusResponseDTO> status() {
        return GlobalRes.success(verificationService.getStatus());
    }
}
