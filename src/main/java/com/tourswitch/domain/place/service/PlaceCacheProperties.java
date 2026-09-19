package com.tourswitch.domain.place.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 자치구별 관광지 목록 캐시 설정.
 * ttlMinutes는 갱신 주기이자 유효기간이며, 한 번 갱신에 자치구 25개 x 콘텐츠 유형 4개로
 * areaBasedList2 100회와 집중률 25회를 사용한다. TourAPI 일일 한도에 맞춰 조정한다.
 */
@ConfigurationProperties(prefix = "place-cache")
public record PlaceCacheProperties(
        boolean enabled,
        int ttlMinutes,
        int initialDelayMinutes
) {
}
