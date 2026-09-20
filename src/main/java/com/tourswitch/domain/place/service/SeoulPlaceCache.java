package com.tourswitch.domain.place.service;

import com.tourswitch.global.spatial.DongNameExtractor;
import com.tourswitch.global.spatial.SpotNameMatcher;
import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import com.tourswitch.global.spatial.HaversineCalculator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자치구별 장소와 집중률을 주기적으로 모아 두는 인메모리 캐시.
 *
 * 화면은 지역을 고르지 않아도 카드마다 자치구명과 혼잡도를 요구하는데, TourAPI는 자치구를
 * 지정해야 그 값을 주므로 요청 시점에 25개 자치구를 도는 것은 응답 시간과 호출 한도 양쪽에서
 * 감당하기 어렵다. 그래서 갱신을 요청 경로 밖으로 빼고 조회는 스냅샷만 읽는다.
 *
 * 관광지 계열(SEARCHABLE)과 코스 부가 후보 계열(EXTRA)을 함께 담는다. 부가 후보를 여기서
 * 고르면 코스를 만들 때마다 locationBasedList2를 부르지 않아도 된다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeoulPlaceCache {

    /** 관광지 검색·후보 구성에 쓰는 콘텐츠 유형. 관광지, 문화시설, 축제공연행사, 레포츠. */
    private static final List<Integer> SEARCHABLE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);
    /** 코스 부가 후보에 쓰는 콘텐츠 유형. 숙박, 쇼핑, 음식점. */
    private static final List<Integer> EXTRA_CONTENT_TYPE_IDS = List.of(32, 38, 39);
    private static final DateTimeFormatter BASE_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KorServiceClient korServiceClient;
    private final TatsCnctrRateClient tatsCnctrRateClient;
    private final PlaceRegionQueryRepository placeRegionQueryRepository;
    private final PlaceCacheProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    private volatile Map<String, List<CachedPlace>> placesByDistrictName = Map.of();

    @Scheduled(initialDelayString = "${place-cache.initial-delay-minutes:1}",
            fixedDelayString = "${place-cache.ttl-minutes:720}",
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
                log.warn("자치구 장소 캐시 갱신 실패. districtName={}", region.districtName(), exception);
            }
        }
        if (collected.isEmpty()) {
            log.warn("자치구 장소 캐시 갱신 결과가 비어 있어 기존 스냅샷을 유지한다.");
            return;
        }
        // 실패한 자치구는 직전 스냅샷을 그대로 둔다. 새로 받은 것만 덮지 않으면
        // 호출 한도에 걸려 일부만 받은 날 캐시가 통째로 줄어든다.
        Map<String, List<CachedPlace>> merged = new LinkedHashMap<>(placesByDistrictName);
        merged.putAll(collected);
        placesByDistrictName = Map.copyOf(merged);
        log.info("자치구 장소 캐시 갱신 완료. 갱신={}건, 실패={}건, 보유 자치구={}건, 장소={}건",
                collected.size(), failedDistrictCount, merged.size(),
                merged.values().stream().mapToInt(List::size).sum());
        // 캐시는 이미 갱신했다. 파생 기준정보 동기화가 실패해도 갱신 자체를 실패로 만들지 않는다.
        try {
            eventPublisher.publishEvent(new PlaceCacheRefreshedEvent());
        } catch (RuntimeException exception) {
            log.warn("장소 캐시 갱신 후처리에 실패했다. 캐시 스냅샷은 유지된다.", exception);
        }
    }

    /**
     * 자치구를 지정하지 않은 관광지 조회용 전체 스냅샷. 부가 후보 계열은 제외한다.
     */
    public List<CachedPlace> findAll() {
        return placesByDistrictName.values().stream()
                .flatMap(List::stream)
                .filter(SeoulPlaceCache::isSearchable)
                .toList();
    }

    /**
     * 자치구 하나의 관광지 스냅샷. 부가 후보 계열은 제외한다.
     */
    public List<CachedPlace> findByDistrictName(String districtName) {
        return placesByDistrictName.getOrDefault(districtName, List.of()).stream()
                .filter(SeoulPlaceCache::isSearchable)
                .toList();
    }

    /**
     * 기준 좌표에서 반경 안에 있는 특정 유형의 장소를 가까운 순으로 돌려준다.
     * 코스 부가 후보를 고를 때 locationBasedList2 대신 쓴다.
     */
    public List<NearbyCachedPlace> findNearby(double latitude, double longitude, int contentTypeId,
                                                double radiusMeters, int limit) {
        return placesByDistrictName.values().stream()
                .flatMap(List::stream)
                .filter(place -> contentTypeId == (place.contentTypeId() == null ? -1 : place.contentTypeId()))
                .map(place -> new NearbyCachedPlace(place, HaversineCalculator.distanceMeters(
                        latitude, longitude, place.latitude(), place.longitude())))
                .filter(nearby -> nearby.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingInt(NearbyCachedPlace::distanceMeters))
                .limit(limit)
                .toList();
    }

    public boolean isEmpty() {
        return placesByDistrictName.isEmpty();
    }

    /**
     * 자치구 안의 동별 대표 좌표. TourAPI가 동 단위 기준정보를 주지 않아, 이미 받아 둔 장소들의
     * 주소에서 동을 뽑고 그 장소들의 좌표를 평균해서 만든다.
     *
     * 기하학적 중심이 아니라 관광지가 모여 있는 지점이라, 반경으로 대체 후보를 찾는 용도에 맞는다.
     */
    public List<DongCentroid> findDongCentroids(String districtName) {
        Map<String, List<CachedPlace>> byDong = new LinkedHashMap<>();
        for (CachedPlace place : placesByDistrictName.getOrDefault(districtName, List.of())) {
            DongNameExtractor.extract(place.address())
                    .ifPresent(dong -> byDong.computeIfAbsent(dong, key -> new ArrayList<>()).add(place));
        }
        return byDong.entrySet().stream()
                .map(entry -> new DongCentroid(entry.getKey(),
                        average(entry.getValue(), CachedPlace::latitude),
                        average(entry.getValue(), CachedPlace::longitude),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(DongCentroid::dongName))
                .toList();
    }

    private static double average(List<CachedPlace> places, ToDoubleFunction<CachedPlace> value) {
        return places.stream().mapToDouble(value).average().orElseThrow();
    }

    private static boolean isSearchable(CachedPlace place) {
        return place.contentTypeId() != null && SEARCHABLE_CONTENT_TYPE_IDS.contains(place.contentTypeId());
    }

    private List<CachedPlace> collectDistrict(PlaceRegionRow region, LocalDate today) {
        Map<String, TourApiCongestionItem> congestionByName = fetchCongestionByName(region, today);
        Map<String, CachedPlace> firstMatchByContentId = new LinkedHashMap<>();
        for (int contentTypeId : allContentTypeIds()) {
            for (TourApiSpotItem item : korServiceClient.areaBasedList2(region.legalDongAreaCode(),
                    region.legalDongDistrictCode(), contentTypeId, null)) {
                TourApiCongestionItem congestion = congestionByName.get(SpotNameMatcher.key(item.title()));
                firstMatchByContentId.putIfAbsent(item.contentId(), new CachedPlace(
                        item.contentId(), item.contentTypeId(), item.title(), item.firstImageUrl(),
                        region.districtName(), item.address(), item.classificationLevel2Code(),
                        item.latitude(), item.longitude(),
                        congestion == null ? null : congestion.concentrationRate()));
            }
        }
        return List.copyOf(new ArrayList<>(firstMatchByContentId.values()));
    }

    private List<Integer> allContentTypeIds() {
        List<Integer> ids = new ArrayList<>(SEARCHABLE_CONTENT_TYPE_IDS);
        ids.addAll(EXTRA_CONTENT_TYPE_IDS);
        return ids;
    }

    private Map<String, TourApiCongestionItem> fetchCongestionByName(PlaceRegionRow region, LocalDate targetDate) {
        String targetBaseYmd = targetDate.format(BASE_YMD_FORMAT);
        Map<String, TourApiCongestionItem> byName = new LinkedHashMap<>();
        for (TourApiCongestionItem item : tatsCnctrRateClient.tatsCnctrRatedList(region.legalDongAreaCode(),
                region.districtCode())) {
            String nameKey = SpotNameMatcher.key(item.touristSpotName());
            // 이름 없는 항목을 빈 키로 넣으면 이름 없는 장소가 전부 그 혼잡도에 붙는다.
            if (!nameKey.isEmpty() && targetBaseYmd.equals(item.baseYmd())) {
                byName.put(nameKey, item);
            }
        }
        return byName;
    }
}
