package com.tourswitch.domain.place.service;

/**
 * 기준 좌표에서의 거리와 함께 돌려주는 캐시 장소.
 */
public record NearbyCachedPlace(CachedPlace place, int distanceMeters) {
}
