package com.tourswitch.domain.congestion.model;

/**
 * 혼잡도 지도에서 표시할 관광지의 식별 정보와 좌표를 표현한다.
 */
public record MapPlace(
        String contentId,
        String name,
        String imageUrl,
        double latitude,
        double longitude
) {
}
