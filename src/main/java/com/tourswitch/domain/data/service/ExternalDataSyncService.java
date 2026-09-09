package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.service.SeoulRealtimeApiClient.SeoulRealtimeSource;
import com.tourswitch.domain.data.service.TourApiClient.AccessibilitySource;
import com.tourswitch.domain.data.service.TourApiClient.CrowdForecastSource;
import com.tourswitch.domain.data.service.TourApiClient.FestivalPeriodSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristOverviewSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristSpotSource;
import com.tourswitch.global.config.ExternalApiProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 외부 API 호출을 모두 마친 뒤 트랜잭션 저장 서비스에 전달한다.
 * 원천 테이블은 자연키 UPSERT, 사라진 관광지는 소프트 비활성화한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalDataSyncService {

    private static final List<String> SEOUL_DISTRICT_CODES = List.of(
            "11110", "11140", "11170", "11200", "11215", "11230", "11260", "11290", "11305",
            "11320", "11350", "11380", "11410", "11440", "11470", "11500", "11530", "11545",
            "11560", "11590", "11620", "11650", "11680", "11710", "11740"
    );

    private final TourApiClient tourApiClient;
    private final SeoulRealtimeApiClient seoulRealtimeApiClient;
    private final ExternalDataPersistenceService persistenceService;
    private final ReferenceDataSyncService referenceDataSyncService;
    private final DerivedDataSyncService derivedDataSyncService;
    private final CrowdLinkService crowdLinkService;
    private final ExternalApiProperties tourApiProperties;

    public int syncTouristSpots() {
        referenceDataSyncService.synchronize();
        List<TouristSpotSource> touristSpots = tourApiClient.fetchSeoulTouristSpots();
        if (touristSpots.isEmpty()) {
            throw new IllegalStateException("TourAPI 관광지 결과가 0건이므로 기존 데이터를 변경하지 않습니다.");
        }
        List<FestivalPeriodSource> festivalPeriods = tourApiClient.fetchCurrentSeoulFestivals(LocalDate.now());
        persistenceService.replaceTouristDataset(touristSpots, festivalPeriods);

        derivedDataSyncService.synchronizeConfirmedDuplicates();
        derivedDataSyncService.synchronizeKeywordLinks();
        derivedDataSyncService.synchronizeAreaLinks();
        log.info("관광지 기본/행사 적재 완료: 관광지={}건, 진행 축제={}건",
                touristSpots.size(), festivalPeriods.size());
        return touristSpots.size();
    }

    public int syncTouristDetails() {
        List<String> contentIds = persistenceService.findActiveCardContentIds();
        if (contentIds.isEmpty()) {
            throw new IllegalStateException("상세정보를 연결할 활성 카드 관광지가 없습니다.");
        }
        List<String> overviewContentIds = persistenceService.findActiveCardContentIdsWithoutOverview();
        int overviewCount = 0;
        if (!overviewContentIds.isEmpty()) {
            List<TouristOverviewSource> overviews = tourApiClient.fetchOverviews(overviewContentIds);
            if (!overviews.isEmpty()) {
                persistenceService.updateTouristOverviews(overviews);
                overviewCount = overviews.size();
            }
        }

        int accessibilityCount = 0;
        if (tourApiProperties.accessibilityEnabled()) {
            List<AccessibilitySource> accessibilityDetails = tourApiClient.fetchAccessibilityDetails(contentIds);
            if (accessibilityDetails.isEmpty()) {
                throw new IllegalStateException("TourAPI 접근성 결과가 0건이므로 기존 데이터를 변경하지 않습니다.");
            }
            persistenceService.replaceAccessibility(accessibilityDetails);
            accessibilityCount = accessibilityDetails.size();
        }
        log.info("관광지 상세 적재 완료: 소개={}건, 접근성={}건, 접근성수집활성={}",
                overviewCount, accessibilityCount, tourApiProperties.accessibilityEnabled());
        return overviewCount + accessibilityCount;
    }

    public int syncCrowdForecasts() {
        List<CrowdForecastSource> forecasts = tourApiClient.fetchCrowdForecasts(SEOUL_DISTRICT_CODES);
        if (forecasts.isEmpty()) {
            throw new IllegalStateException("TourAPI 집중률 결과가 0건이므로 기존 데이터를 변경하지 않습니다.");
        }
        persistenceService.upsertCrowdForecasts(forecasts);
        crowdLinkService.synchronizeLinks();
        derivedDataSyncService.synchronizeCrowdGrades();
        log.info("관광지 집중률 적재 완료: {}건", forecasts.size());
        return forecasts.size();
    }

    public int syncSeoulRealtime() {
        referenceDataSyncService.synchronize();
        List<String> areaCodes = persistenceService.findRealtimeAreaCodes();
        List<SeoulRealtimeSource> snapshots = new ArrayList<>();
        for (String areaCode : areaCodes) {
            seoulRealtimeApiClient.fetch(areaCode).ifPresent(snapshots::add);
        }
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("서울 실시간 인구 결과가 0건이므로 기존 데이터를 변경하지 않습니다.");
        }
        persistenceService.saveRealtimeSnapshots(snapshots);
        log.info("서울 실시간 인구/예측 적재 완료: 요청={}곳, 응답={}곳", areaCodes.size(), snapshots.size());
        return snapshots.size();
    }
}
