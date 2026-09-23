package com.tourswitch.domain.place.model;

/**
 * TourAPI에서 조회한 찜 관광지의 최신 표시 정보를 표현한다.
 */
public record FavoritePlace(String contentId, String name, String imageUrl, String address) {
}
