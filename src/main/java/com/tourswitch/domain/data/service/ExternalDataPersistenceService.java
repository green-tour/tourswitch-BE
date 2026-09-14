package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.AccessibilityUpsertCommand;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.CrowdForecastUpsertCommand;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.TouristSpotUpsertCommand;
import com.tourswitch.domain.data.service.TourApiClient.AccessibilitySource;
import com.tourswitch.domain.data.service.TourApiClient.CrowdForecastSource;
import com.tourswitch.domain.data.service.TourApiClient.FestivalPeriodSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristOverviewSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristSpotSource;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExternalDataPersistenceService {

    private static final BigDecimal SEOUL_MINIMUM_LATITUDE = new BigDecimal("37");
    private static final BigDecimal SEOUL_MAXIMUM_LATITUDE = new BigDecimal("38");
    private static final BigDecimal SEOUL_MINIMUM_LONGITUDE = new BigDecimal("126");
    private static final BigDecimal SEOUL_MAXIMUM_LONGITUDE = new BigDecimal("128");

    private final ExternalDataSyncRepository repository;
    private final PlaceNameMatcher placeNameMatcher;
    private final AccessibilityClassifier accessibilityClassifier;

    @Transactional
    public void replaceTouristDataset(
            List<TouristSpotSource> touristSpots,
            List<FestivalPeriodSource> festivalPeriods
    ) {
        List<TouristSpotUpsertCommand> commands = touristSpots.stream()
                .map(source -> new TouristSpotUpsertCommand(
                        source,
                        placeNameMatcher.normalize(source.title()),
                        placeNameMatcher.normalize(source.address()),
                        isCoordinateValid(source)
                ))
                .toList();
        repository.replaceTouristSpots(commands);
        repository.replaceFestivalPeriods(festivalPeriods);
    }

    @Transactional(readOnly = true)
    public List<String> findActiveCardContentIds() {
        return repository.findActiveCardContentIds();
    }

    @Transactional(readOnly = true)
    public List<String> findActiveCardContentIdsWithoutOverview() {
        return repository.findActiveCardContentIdsWithoutOverview();
    }

    @Transactional
    public void updateTouristOverviews(List<TouristOverviewSource> overviews) {
        repository.updateOverviews(overviews);
    }

    @Transactional
    public void upsertAccessibility(List<AccessibilitySource> accessibilityDetails) {
        List<AccessibilityUpsertCommand> commands = accessibilityDetails.stream()
                .map(source -> new AccessibilityUpsertCommand(
                        source,
                        accessibilityClassifier.isAccessible(source.wheelchairDescription()),
                        accessibilityClassifier.isAccessible(source.strollerDescription())
                ))
                .toList();
        repository.upsertAccessibility(commands);
    }

    @Transactional
    public void upsertCrowdForecasts(List<CrowdForecastSource> forecasts) {
        repository.upsertCrowdForecasts(forecasts.stream()
                .map(source -> new CrowdForecastUpsertCommand(
                        source,
                        placeNameMatcher.normalize(source.attractionName())
                ))
                .toList());
        repository.deleteExpiredCrowdForecasts();
    }

    private boolean isCoordinateValid(TouristSpotSource source) {
        return source.latitude().compareTo(SEOUL_MINIMUM_LATITUDE) >= 0
                && source.latitude().compareTo(SEOUL_MAXIMUM_LATITUDE) <= 0
                && source.longitude().compareTo(SEOUL_MINIMUM_LONGITUDE) >= 0
                && source.longitude().compareTo(SEOUL_MAXIMUM_LONGITUDE) <= 0;
    }
}
