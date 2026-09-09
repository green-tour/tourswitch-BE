package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.DataSyncMetrics;
import com.tourswitch.domain.data.response.DataSyncStatusResponseDTO;
import com.tourswitch.global.config.ExternalApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataSyncVerificationService {

    private final ExternalDataSyncRepository repository;
    private final ExternalApiProperties tourApiProperties;

    @Transactional(readOnly = true)
    public DataSyncStatusResponseDTO getStatus() {
        DataSyncMetrics metrics = repository.findDataSyncMetrics();
        return DataSyncStatusResponseDTO.of(
                isCoreDataComplete(metrics),
                tourApiProperties.accessibilityEnabled(),
                metrics
        );
    }

    @Transactional(readOnly = true)
    public int countIntegrityIssues() {
        DataSyncMetrics metrics = repository.findDataSyncMetrics();
        return metrics.touristSpotsMissingRegion()
                + metrics.invalidAreaBoundaries()
                + metrics.staleCrowdLinks();
    }

    private boolean isCoreDataComplete(DataSyncMetrics metrics) {
        return metrics.regions() == 25
                && metrics.activeKeywords() == 12
                && metrics.keywordClassifications() == 31
                && metrics.activeTouristSpots() > 0
                && metrics.touristSpotsMissingRegion() == 0
                && metrics.keywordLinks() > 0
                && metrics.futureCrowdForecasts() > 0
                && metrics.crowdLinks() > 0
                && metrics.staleCrowdLinks() == 0
                && metrics.futureCrowdGradeThresholds() > 0
                && metrics.realtimeAreas() == 121
                && metrics.invalidAreaBoundaries() == 0
                && metrics.areaLinks() > 0
                && metrics.duplicateLinks() == 3;
    }
}
