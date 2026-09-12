package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * areaBasedList2/locationBasedList2 공통 응답 항목. distanceMeters는 locationBasedList2에서만 채워진다.
 */
public record TourApiSpotItem(
        String contentId,
        Integer contentTypeId,
        String title,
        String address,
        String firstImageUrl,
        double latitude,
        double longitude,
        String classificationLevel1Code,
        String classificationLevel2Code,
        String classificationLevel3Code,
        Double distanceMeters
) {

    static TourApiSpotItem from(JsonNode node) {
        return new TourApiSpotItem(
                node.path("contentid").asText(),
                parseIntOrNull(node.path("contenttypeid").asText(null)),
                node.path("title").asText(),
                blankToNull(node.path("addr1").asText(null)),
                blankToNull(node.path("firstimage").asText(null)),
                node.path("mapy").asDouble(),
                node.path("mapx").asDouble(),
                blankToNull(node.path("lclsSystm1").asText(null)),
                blankToNull(node.path("lclsSystm2").asText(null)),
                blankToNull(node.path("lclsSystm3").asText(null)),
                node.has("dist") ? node.path("dist").asDouble() : null);
    }

    private static Integer parseIntOrNull(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
