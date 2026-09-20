package com.tourswitch.domain.place.service;

/**
 * 장소 캐시가 새 스냅샷으로 바뀐 뒤 발행한다. 캐시에서 파생되는 기준정보를 쓰는 쪽이
 * 갱신 시점을 짐작하지 않고 이어받게 한다.
 */
public record PlaceCacheRefreshedEvent() {
}
