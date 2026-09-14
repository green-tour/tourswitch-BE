package com.tourswitch.domain.place.response;

import java.time.LocalDateTime;
import java.util.List;

public record PlaceDetailResponseDTO(
        String id,
        String name,
        String regionName,
        String summary,
        String imageUrl,
        String address,
        double latitude,
        double longitude,
        PlaceAccessibilityResponseDTO accessibility,
        PlaceCongestionResponseDTO congestion,
        PlaceRealtimeCongestionResponseDTO realtimeCongestion,
        List<PlaceForecastResponseDTO> forecasts,
        LocalDateTime dataSyncedAt,
        String dataSource
) {
}
