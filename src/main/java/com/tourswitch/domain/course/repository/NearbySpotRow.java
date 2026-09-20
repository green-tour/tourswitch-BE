package com.tourswitch.domain.course.repository;

/**
 * title/imageUrl은 부가 후보 카드에 그대로 노출하기 위한 스냅샷이다.
 * 설명은 목록 응답에 없으므로 사용자가 후보를 고를 때 상세 조회로 채운다.
 */
public record NearbySpotRow(String contentId, int distanceMeters, String title, String imageUrl) {
}
