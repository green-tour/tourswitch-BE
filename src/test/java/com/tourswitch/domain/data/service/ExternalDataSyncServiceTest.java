package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.data.service.TourApiClient.FestivalPeriodSource;
import com.tourswitch.domain.data.service.TourApiClient.TouristSpotSource;
import com.tourswitch.global.config.ExternalApiProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalDataSyncServiceTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private ExternalDataPersistenceService persistenceService;

    @Mock
    private ReferenceDataSyncService referenceDataSyncService;

    @Mock
    private DerivedDataSyncService derivedDataSyncService;

    @Mock
    private CrowdLinkService crowdLinkService;

    private ExternalDataSyncService service;

    @BeforeEach
    void setUp() {
        service = new ExternalDataSyncService(
                tourApiClient,
                persistenceService,
                referenceDataSyncService,
                derivedDataSyncService,
                crowdLinkService,
                new ExternalApiProperties("key", "ETC", "test", false, 1)
        );
    }

    @Test
    void syncTouristSpots_emptySource_doesNotModifyTouristDataset() {
        // Given
        when(tourApiClient.fetchSeoulTouristSpots()).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(service::syncTouristSpots)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("0건");
        verify(persistenceService, never()).replaceTouristDataset(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void syncTouristSpots_validSource_persistsThenRefreshesDerivedData() {
        // Given
        TouristSpotSource touristSpot = new TouristSpotSource(
                "1", 12, "경복궁", "서울특별시 종로구", new BigDecimal("37.5"),
                new BigDecimal("126.9"), null, "HS", "HS01", "HS0101", "11110"
        );
        FestivalPeriodSource festival = new FestivalPeriodSource(
                "2", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );
        when(tourApiClient.fetchSeoulTouristSpots()).thenReturn(List.of(touristSpot));
        when(tourApiClient.fetchCurrentSeoulFestivals(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(festival));

        // When
        service.syncTouristSpots();

        // Then
        verify(persistenceService).replaceTouristDataset(List.of(touristSpot), List.of(festival));
        verify(derivedDataSyncService).synchronizeConfirmedDuplicates();
        verify(derivedDataSyncService).synchronizeKeywordLinks();
        verify(derivedDataSyncService).synchronizeAreaLinks();
    }
}
