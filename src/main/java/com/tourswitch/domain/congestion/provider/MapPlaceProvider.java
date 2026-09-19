package com.tourswitch.domain.congestion.provider;

import com.tourswitch.domain.congestion.model.MapPlace;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 서울 관광지를 TourAPI에서 조회하고 지도 조회를 위해 짧게 메모리 캐시한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MapPlaceProvider {

    private static final String SEOUL_AREA_CODE = "11";
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);
    private static final Duration FAILURE_RETRY_DELAY = Duration.ofMinutes(1);
    private static final List<Integer> CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);

    private final KorServiceClient korServiceClient;
    private volatile PlaceCache placeCache = new PlaceCache(Instant.EPOCH, List.of());

    /**
     * 캐시가 유효하면 캐시를 반환하고 만료됐으면 서울 관광지를 다시 조회한다.
     */
    public List<MapPlace> findSeoulPlaces() {
        PlaceCache current = placeCache;
        if (Instant.now().isBefore(current.expiresAt())) {
            return current.places();
        }
        return refresh(current);
    }

    /**
     * 관광 유형별 TourAPI 결과를 contentId 기준으로 중복 제거해 캐시한다.
     */
    private synchronized List<MapPlace> refresh(PlaceCache previous) {
        Instant now = Instant.now();
        if (now.isBefore(placeCache.expiresAt())) {
            return placeCache.places();
        }
        try {
            Map<String, MapPlace> placesByContentId = new LinkedHashMap<>();
            for (int contentTypeId : CONTENT_TYPE_IDS) {
                for (TourApiSpotItem item : korServiceClient.areaBasedList2(
                        SEOUL_AREA_CODE, null, contentTypeId, null)) {
                    if (isMapPlace(item)) {
                        MapPlace place = toMapPlace(item);
                        placesByContentId.putIfAbsent(place.contentId(), place);
                    }
                }
            }
            List<MapPlace> places = List.copyOf(placesByContentId.values());
            placeCache = new PlaceCache(now.plus(CACHE_DURATION), places);
            return places;
        } catch (RuntimeException exception) {
            log.error("지도 관광지 실시간 조회에 실패해 최근 캐시를 사용합니다.", exception);
            placeCache = new PlaceCache(now.plus(FAILURE_RETRY_DELAY), previous.places());
            return previous.places();
        }
    }

    /**
     * TourAPI 관광지가 지도에 표시할 식별자와 서울 범위 좌표를 가졌는지 확인한다.
     */
    private boolean isMapPlace(TourApiSpotItem item) {
        return item.contentId() != null && !item.contentId().isBlank()
                && item.title() != null && !item.title().isBlank()
                && item.latitude() >= 37.0 && item.latitude() <= 38.0
                && item.longitude() >= 126.0 && item.longitude() <= 128.0;
    }

    /**
     * 외부 TourAPI 응답을 혼잡도 지도 도메인 모델로 변환한다.
     */
    private MapPlace toMapPlace(TourApiSpotItem item) {
        return new MapPlace(
                item.contentId(),
                item.title(),
                item.firstImageUrl(),
                item.latitude(),
                item.longitude()
        );
    }

    /**
     * 관광지 캐시 만료 시각과 불변 관광지 목록을 보관한다.
     */
    private static final class PlaceCache {

        private final Instant expiresAt;
        private final List<MapPlace> places;

        private PlaceCache(Instant expiresAt, List<MapPlace> places) {
            this.expiresAt = expiresAt;
            this.places = places;
        }

        /**
         * 캐시 만료 시각을 반환한다.
         */
        private Instant expiresAt() {
            return expiresAt;
        }

        /**
         * 캐시된 불변 관광지 목록을 반환한다.
         */
        private List<MapPlace> places() {
            return places;
        }
    }
}
