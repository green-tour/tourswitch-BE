package com.tourswitch.domain.data.repository;

public record CrowdLinkSaveCommand(
        Long touristSpotId,
        String attractionName,
        String districtCode,
        String matchMethod
) {
}
