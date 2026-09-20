package com.tourswitch.domain.place.service;

/**
 * 캐시에서 도출한 동 하나. placeCount는 좌표 평균에 쓰인 장소 수로, 대표성이 얼마나 되는지를 나타낸다.
 */
public record DongCentroid(String dongName, double centerLatitude, double centerLongitude, int placeCount) {
}
