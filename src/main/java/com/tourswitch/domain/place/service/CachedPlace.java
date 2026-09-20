package com.tourswitch.domain.place.service;

import java.math.BigDecimal;

/**
 * 캐시에 담는 장소 한 건. 관광지 계열뿐 아니라 음식점·쇼핑·숙박도 함께 담으므로
 * contentTypeId로 용도를 구분한다.
 *
 * classificationLevel2Code(lclsSystm2)는 카테고리 필터를 TourAPI 재호출 없이 처리하기 위해,
 * 좌표는 코스 부가 후보를 반경으로 고르기 위해 보관한다.
 */
public record CachedPlace(
        String contentId,
        Integer contentTypeId,
        String title,
        String imageUrl,
        String districtName,
        String classificationLevel2Code,
        double latitude,
        double longitude,
        BigDecimal concentrationRate
) {
}
