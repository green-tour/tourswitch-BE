package com.tourswitch.domain.place.service;

import java.math.BigDecimal;

/**
 * 캐시에 담는 관광지 한 건. 카테고리 필터를 TourAPI 재호출 없이 메모리에서 처리하기 위해
 * classificationLevel2Code(lclsSystm2)를 함께 보관한다.
 */
public record CachedPlace(
        String contentId,
        String title,
        String imageUrl,
        String districtName,
        String classificationLevel2Code,
        BigDecimal concentrationRate
) {
}
