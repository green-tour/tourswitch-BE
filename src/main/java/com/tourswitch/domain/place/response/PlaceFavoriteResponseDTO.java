package com.tourswitch.domain.place.response;

/**
 * 현재 회원의 관광지 찜 여부를 전달한다.
 */
public record PlaceFavoriteResponseDTO(String contentId, boolean favorite) {

    /**
     * 관광지 식별자와 찜 여부로 응답을 생성한다.
     */
    public static PlaceFavoriteResponseDTO of(String contentId, boolean favorite) {
        return new PlaceFavoriteResponseDTO(contentId, favorite);
    }
}
