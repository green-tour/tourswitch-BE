package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;

public record TourApiSpotDetail(String contentId, String title, String overview, String address,
                                 String firstImageUrl, double latitude, double longitude) {

    static TourApiSpotDetail from(JsonNode node) {
        return new TourApiSpotDetail(
                node.path("contentid").asText(),
                node.path("title").asText(),
                blankToNull(node.path("overview").asText(null)),
                blankToNull(node.path("addr1").asText(null)),
                blankToNull(node.path("firstimage").asText(null)),
                node.path("mapy").asDouble(),
                node.path("mapx").asDouble());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
