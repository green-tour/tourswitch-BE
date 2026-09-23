package com.tourswitch.domain.place.response;

import com.tourswitch.domain.place.model.FavoritePlace;

/**
 * 찜 목록에 표시할 관광지의 최신 정보를 전달한다.
 */
public record FavoritePlaceResponseDTO(String id, String name, String imageUrl, String address) {

    /**
     * 찜 관광지 모델을 API 응답으로 변환한다.
     */
    public static FavoritePlaceResponseDTO from(FavoritePlace place) {
        return new FavoritePlaceResponseDTO(place.contentId(), place.name(), place.imageUrl(), place.address());
    }
}
