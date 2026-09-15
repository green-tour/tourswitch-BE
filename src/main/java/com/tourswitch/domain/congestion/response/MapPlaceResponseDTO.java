package com.tourswitch.domain.congestion.response;

import com.tourswitch.domain.congestion.model.MapPlace;

/**
 * 지도 영역에 연결된 관광지의 마커 및 상세 이동 정보를 제공한다.
 */
public record MapPlaceResponseDTO(
        String contentId,
        String name,
        String imageUrl,
        double latitude,
        double longitude,
        String detailPath
) {

    /**
     * 지도 관광지 모델을 프론트 공개 응답으로 변환한다.
     */
    public static MapPlaceResponseDTO from(MapPlace place) {
        return new MapPlaceResponseDTO(
                place.contentId(),
                place.name(),
                place.imageUrl(),
                place.latitude(),
                place.longitude(),
                "/api/places/" + place.contentId()
        );
    }
}
