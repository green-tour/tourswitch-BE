package com.tourswitch.domain.metadata.model;

import java.util.Arrays;

public enum KeywordCode {
    EXHIBITION_MUSEUM("전시·박물관"),
    FESTIVAL_EVENT("축제·행사"),
    CITY_PARK("도시공원"),
    HISTORICAL_RELIC("역사유적"),
    LEISURE_SPORTS("레저스포츠"),
    EXPERIENCE("체험"),
    PERFORMANCE("공연"),
    NATURE_MOUNTAIN("자연·산"),
    RELIGIOUS_SITE("종교성지"),
    STREET_TRAIL("골목·거리·둘레길"),
    LANDMARK_VIEW("랜드마크·전망"),
    THEME_PARK("테마파크");

    private final String keywordName;

    KeywordCode(String keywordName) {
        this.keywordName = keywordName;
    }

    public static String fromKeywordName(String keywordName) {
        return Arrays.stream(values())
                .filter(value -> value.keywordName.equals(keywordName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 키워드입니다: " + keywordName))
                .name();
    }
}
