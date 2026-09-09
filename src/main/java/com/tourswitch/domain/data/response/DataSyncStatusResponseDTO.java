package com.tourswitch.domain.data.response;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.DataSyncMetrics;

public record DataSyncStatusResponseDTO(
        boolean coreDataComplete,
        boolean accessibilityCollectionEnabled,
        DataSyncMetrics metrics
) {

    public static DataSyncStatusResponseDTO of(
            boolean coreDataComplete,
            boolean accessibilityCollectionEnabled,
            DataSyncMetrics metrics
    ) {
        return new DataSyncStatusResponseDTO(coreDataComplete, accessibilityCollectionEnabled, metrics);
    }
}
