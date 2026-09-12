package com.tourswitch.domain.place.response;

import java.math.BigDecimal;

public record PlaceDetailResponseDTO(
        String id,
        String name,
        String regionName,
        String summary,
        String imageUrl,
        String address,
        double latitude,
        double longitude,
        PlaceCongestionResponseDTO congestion
) {

    public static PlaceDetailResponseDTO of(String contentId, String title, String regionName, String overview,
                                             String imageUrl, String address, double latitude, double longitude,
                                             String concentrationGrade, BigDecimal concentrationRate) {
        return new PlaceDetailResponseDTO(contentId, title, regionName, overview, imageUrl, address, latitude,
                longitude, PlaceCongestionResponseDTO.of(concentrationGrade, concentrationRate));
    }
}
