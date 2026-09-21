package com.tourswitch.domain.place.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 관광지의 특정 날짜 예상 집중률을 표현한다.
 */
public record PlaceCrowdForecast(
        LocalDate forecastDate,
        BigDecimal concentrationRate
) {
}
