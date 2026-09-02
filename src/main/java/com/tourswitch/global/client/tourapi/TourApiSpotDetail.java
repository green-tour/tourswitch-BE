package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;

public record TourApiSpotDetail(String contentId, String title, String overview) {

    static TourApiSpotDetail from(JsonNode node) {
        return new TourApiSpotDetail(
                node.path("contentid").asText(),
                node.path("title").asText(),
                blankToNull(node.path("overview").asText(null)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
