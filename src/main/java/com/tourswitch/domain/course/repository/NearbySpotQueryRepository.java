package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.place.service.SeoulPlaceCache;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 부가 카테고리 후보(DB설계 8.3절): 기준 지점 반경 이내에서 거리순으로 상위 N개를 찾는다.
 *
 * 자치구별 장소 캐시에 음식점·쇼핑·숙박까지 담고 있으므로 캐시에서 거리 계산으로 고른다.
 * 코스를 만들 때마다 locationBasedList2를 부르지 않아도 되고 호출 한도를 아낀다.
 * 기동 직후처럼 캐시가 비어 있으면 기존대로 TourAPI를 직접 호출한다.
 */
@Repository
@RequiredArgsConstructor
public class NearbySpotQueryRepository {

    private final KorServiceClient korServiceClient;
    private final SeoulPlaceCache seoulPlaceCache;

    public List<NearbySpotRow> findNearby(double anchorLatitude, double anchorLongitude, int contentTypeId,
                                           double radiusMeters, int limit) {
        if (!seoulPlaceCache.isEmpty()) {
            return seoulPlaceCache.findNearby(anchorLatitude, anchorLongitude, contentTypeId, radiusMeters, limit)
                    .stream()
                    .map(nearby -> new NearbySpotRow(nearby.place().contentId(), nearby.distanceMeters(),
                            nearby.place().title(), nearby.place().imageUrl()))
                    .toList();
        }
        return korServiceClient.locationBasedList2(anchorLatitude, anchorLongitude, (int) radiusMeters, contentTypeId)
                .stream()
                .limit(limit)
                .map(item -> new NearbySpotRow(item.contentId(),
                        item.distanceMeters() == null ? 0 : item.distanceMeters().intValue(),
                        item.title(), item.firstImageUrl()))
                .toList();
    }
}
