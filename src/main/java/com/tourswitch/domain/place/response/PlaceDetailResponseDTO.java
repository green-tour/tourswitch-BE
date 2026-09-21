package com.tourswitch.domain.place.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상세 관광지 화면에 표시할 기본 정보, 현재 혼잡도와 일주일 예측을 제공한다.
 */
public record PlaceDetailResponseDTO(
        String id,
        String name,
        String regionName,
        String summary,
        String imageUrl,
        String address,
        double latitude,
        double longitude,
        PlaceCongestionResponseDTO congestion,
        List<PlaceCrowdForecastResponseDTO> weeklyForecast
) {

    /**
     * TourAPI 상세 결과와 혼잡도 정보를 프론트 응답으로 변환한다.
     */
    public static PlaceDetailResponseDTO of(String contentId, String title, String regionName, String overview,
                                             String imageUrl, String address, double latitude, double longitude,
                                             String concentrationGrade, BigDecimal concentrationRate,
                                             List<PlaceCrowdForecastResponseDTO> weeklyForecast) {
        return new PlaceDetailResponseDTO(contentId, title, regionName, overview, imageUrl, address, latitude,
                longitude, PlaceCongestionResponseDTO.of(concentrationGrade, concentrationRate), weeklyForecast);
    }
}
