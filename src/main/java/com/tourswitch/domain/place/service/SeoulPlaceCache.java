package com.tourswitch.domain.place.service;

import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자치구별 관광지 목록과 집중률을 주기적으로 모아 두는 인메모리 캐시.
 *
 * 화면은 지역을 고르지 않아도 카드마다 자치구명과 혼잡도를 요구하는데, TourAPI는 자치구를
 * 지정해야 그 값을 주므로 요청 시점에 25개 자치구를 도는 것은 응답 시간과 호출 한도 양쪽에서
 * 감당하기 어렵다. 그래서 갱신을 요청 경로 밖으로 빼고 조회는 스냅샷만 읽는다.
 *
 * 카테고리 필터도 캐시에서 처리한다. areaBasedList2를 분류코드마다 다시 부르지 않고
 * 보관해 둔 classificationLevel2Code로 거른다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeoulPlaceCache {

    private static final List<Integer> SEARCHABLE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);
    private static final DateTimeFormatter BASE_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KorServiceClient korServiceClient;
    private final TatsCnctrRateClient tatsCnctrRateClient;
    private final PlaceRegionQueryRepository placeRegionQueryRepository;
    private final PlaceCacheProperties properties;

    private volatile Map<String, List<CachedPlace>> placesByDistrictName = Map.of();

    @Scheduled(initialDelayString = "${place-cache.initial-delay-minutes:1}",
            fixedDelayString = "${place-cache.ttl-minutes:30}",
            timeUnit = TimeUnit.MINUTES)
    public void refresh() {
        if (!properties.enabled()) return;
        LocalDate today = LocalDate.now();
        Map<String, List<CachedPlace>> collected = new LinkedHashMap<>();
        int failedDistrictCount = 0;
        for (PlaceRegionRow region : placeRegionQueryRepository.findAll()) {
            try {
                collected.put(region.districtName(), collectDistrict(region, today));
            } catch (RuntimeException exception) {
                failedDistrictCount++;
                log.warn("자치구 관광지 캐시 갱신 실패. districtName={}", region.districtName(), exception);
            }
        }
        if (collected.isEmpty()) {
            log.warn("자치구 관광지 캐시 갱신 결과가 비어 있어 기존 스냅샷을 유지한다.");
            return;
        }
        placesByDistrictName = Map.copyOf(collected);
        log.info("자치구 관광지 캐시 갱신 완료. 자치구={}건, 실패={}건, 관광지={}건",
                collected.size(), failedDistrictCount,
                collected.values().stream().mapToInt(List::size).sum());
    }

    /**
     * 자치구를 지정하지 않은 조회용 전체 스냅샷. 아직 한 번도 채워지지 않았으면 비어 있다.
     */
    public List<CachedPlace> findAll() {
        return placesByDistrictName.values().stream().flatMap(List::stream).toList();
    }

    /**
     * 자치구 하나의 스냅샷. 해당 자치구가 아직 캐시에 없으면 비어 있다.
     */
    public List<CachedPlace> findByDistrictName(String districtName) {
        return placesByDistrictName.getOrDefault(districtName, List.of());
    }

    public boolean isEmpty() {
        return placesByDistrictName.isEmpty();
    }

    private List<CachedPlace> collectDistrict(PlaceRegionRow region, LocalDate today) {
        Map<String, TourApiCongestionItem> congestionByName = fetchCongestionByName(region, today);
        Map<String, CachedPlace> firstMatchByContentId = new LinkedHashMap<>();
        for (int contentTypeId : SEARCHABLE_CONTENT_TYPE_IDS) {
            for (TourApiSpotItem item : korServiceClient.areaBasedList2(region.legalDongAreaCode(),
                    region.legalDongDistrictCode(), contentTypeId, null)) {
                TourApiCongestionItem congestion = congestionByName.get(item.title());
                firstMatchByContentId.putIfAbsent(item.contentId(), new CachedPlace(
                        item.contentId(), item.title(), item.firstImageUrl(), region.districtName(),
                        item.classificationLevel2Code(),
                        congestion == null ? null : congestion.concentrationRate()));
            }
        }
        return List.copyOf(new ArrayList<>(firstMatchByContentId.values()));
    }

    private Map<String, TourApiCongestionItem> fetchCongestionByName(PlaceRegionRow region, LocalDate targetDate) {
        String targetBaseYmd = targetDate.format(BASE_YMD_FORMAT);
        Map<String, TourApiCongestionItem> byName = new LinkedHashMap<>();
        for (TourApiCongestionItem item : tatsCnctrRateClient.tatsCnctrRatedList(region.legalDongAreaCode(),
                region.districtCode())) {
            if (targetBaseYmd.equals(item.baseYmd())) {
                byName.put(item.touristSpotName(), item);
            }
        }
        return byName;
    }
}
