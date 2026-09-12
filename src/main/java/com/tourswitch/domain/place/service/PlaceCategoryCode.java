package com.tourswitch.domain.place.service;

import java.util.Map;

/**
 * FE의 관광지 카테고리 필터 코드(src/constants/placeCategories.js)를 keyword.keyword_name과
 * 맞춘다. 두 목록은 이름과 순서가 1:1로 일치하도록 관리한다(2026-08 카테고리 필터링 화면 반영분).
 */
final class PlaceCategoryCode {

    private static final Map<String, String> CODE_TO_KEYWORD_NAME = Map.ofEntries(
            Map.entry("EXHIBITION_MUSEUM", "전시·박물관"),
            Map.entry("FESTIVAL_EVENT", "축제·행사"),
            Map.entry("CITY_PARK", "도시공원"),
            Map.entry("HISTORICAL_RELIC", "역사유적"),
            Map.entry("LEISURE_SPORTS", "레저스포츠"),
            Map.entry("EXPERIENCE", "체험"),
            Map.entry("PERFORMANCE", "공연"),
            Map.entry("NATURE_MOUNTAIN", "자연·산"),
            Map.entry("RELIGIOUS_SITE", "종교성지"),
            Map.entry("STREET_TRAIL", "골목·거리·둘레길"),
            Map.entry("LANDMARK_VIEW", "랜드마크·전망"),
            Map.entry("THEME_PARK", "테마파크"));

    private PlaceCategoryCode() {
    }

    static String toKeywordName(String code) {
        return CODE_TO_KEYWORD_NAME.get(code);
    }
}
