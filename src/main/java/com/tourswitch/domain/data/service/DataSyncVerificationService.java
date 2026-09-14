package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.DataSyncMetrics;
import com.tourswitch.domain.data.response.DataSyncStatusResponseDTO;
import com.tourswitch.global.client.tourapi.TourApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataSyncVerificationService {

    private final ExternalDataSyncRepository repository;
    private final TourApiProperties tourApiProperties;

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
        int accessibilityIssue = tourApiProperties.accessibilityEnabled() && metrics.accessibilityRows() == 0 ? 1 : 0;
        return metrics.touristSpotsMissingRegion()
                + metrics.touristSpotsMissingClassification()
                + metrics.invalidAreaBoundaries()
                + metrics.staleCrowdLinks()
                + Math.max(0, 121 - metrics.recentlyCollectedRealtimeAreas())
                + accessibilityIssue;
    }

    private boolean isCoreDataComplete(DataSyncMetrics metrics) {
        return metrics.regions() == 25
                && metrics.activeKeywords() == 12
                && metrics.keywordClassifications() == 31
                && metrics.activeTouristSpots() > 0
                && metrics.touristSpotsMissingRegion() == 0
                && metrics.touristSpotsMissingClassification() == 0
                && (!tourApiProperties.accessibilityEnabled() || metrics.accessibilityRows() > 0)
                && metrics.keywordLinks() > 0
                && metrics.futureCrowdForecasts() > 0
                && metrics.crowdLinks() > 0
                && metrics.staleCrowdLinks() == 0
                && metrics.futureCrowdGradeThresholds() > 0
                && metrics.realtimeAreas() == 121
                && metrics.invalidAreaBoundaries() == 0
                && metrics.recentlyCollectedRealtimeAreas() == 121
                && metrics.areaLinks() > 0
                && metrics.duplicateLinks() == 3;
    }
}
