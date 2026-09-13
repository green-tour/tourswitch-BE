package com.tourswitch.domain.place.response;

import java.math.BigDecimal;

public record PlaceSummaryResponseDTO(
        String id,
        String name,
        String regionName,
        String imageUrl,
        PlaceCongestionResponseDTO congestion
) {

    public static PlaceSummaryResponseDTO of(String contentId, String title, String regionName, String imageUrl,
                                              String concentrationGrade, BigDecimal concentrationRate) {
        return new PlaceSummaryResponseDTO(contentId, title, regionName, imageUrl,
                PlaceCongestionResponseDTO.of(concentrationGrade, concentrationRate));
    }
}
