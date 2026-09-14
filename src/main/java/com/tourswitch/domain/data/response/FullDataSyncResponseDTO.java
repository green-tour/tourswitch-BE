package com.tourswitch.domain.data.response;

public record FullDataSyncResponseDTO(
        int touristSpots,
        int touristDetails,
        int crowdForecasts,
        DerivedDataSyncResponseDTO derivedData,
        DataSyncStatusResponseDTO status
) {
}
