package com.tourswitch.domain.place.model;

import java.time.LocalDate;
import java.util.List;

/**
 * TourAPI 집중률 예측을 조회할 관광지명, 별칭과 자치구 조건을 표현한다.
 */
public record PlaceCrowdForecastQuery(
        String areaCode,
        String districtCode,
        String placeName,
        List<String> aliasNames,
        LocalDate startDate
) {
}
