package com.tourswitch.domain.place.response;

import com.tourswitch.domain.place.model.PlaceCrowdForecast;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 상세 관광지 화면의 혼잡도 선 그래프에 사용할 일자별 예측 응답이다.
 */
public record PlaceCrowdForecastResponseDTO(
        LocalDate date,
        BigDecimal rate,
        String level
) {

    /**
     * 장소 도메인 예측값을 API 응답 형식으로 변환한다.
     */
    public static PlaceCrowdForecastResponseDTO of(PlaceCrowdForecast forecast, String level) {
        return new PlaceCrowdForecastResponseDTO(forecast.forecastDate(), forecast.concentrationRate(), level);
    }
}
