package com.tourswitch.domain.course.repository;

import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 부가 카테고리 후보(DB설계 8.3절): 기준 지점 반경 이내에서 거리순으로 상위 N개를 TourAPI에서
 * 실시간으로 찾는다(TourAPI 실시간전환 계획 문서 5.3절). locationBasedList2 응답이 이미 거리순
 * 정렬(arrange=E)과 dist 필드를 함께 주므로 별도 거리 계산이 필요 없다.
 */
@Repository
@RequiredArgsConstructor
public class NearbySpotQueryRepository {

    private final KorServiceClient korServiceClient;

    public List<NearbySpotRow> findNearby(double anchorLatitude, double anchorLongitude, int contentTypeId,
                                           double radiusMeters, int limit) {
        return korServiceClient.locationBasedList2(anchorLatitude, anchorLongitude, (int) radiusMeters, contentTypeId)
                .stream()
                .limit(limit)
                .map(item -> new NearbySpotRow(item.contentId(),
                        item.distanceMeters() == null ? 0 : item.distanceMeters().intValue()))
                .toList();
    }
}
