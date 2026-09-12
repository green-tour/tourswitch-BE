package com.tourswitch.domain.place.response;

public record PlaceSummaryResponseDTO(
        String id,
        String name,
        String regionName,
        String imageUrl,
        String concentrationGrade
) {

    public static PlaceSummaryResponseDTO of(String contentId, String title, String regionName, String imageUrl,
                                              String concentrationGrade) {
        return new PlaceSummaryResponseDTO(contentId, title, regionName, imageUrl, concentrationGrade);
    }
}
