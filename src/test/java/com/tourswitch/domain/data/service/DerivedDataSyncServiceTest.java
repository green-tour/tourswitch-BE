package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.data.repository.CrowdLinkRepository;
import com.tourswitch.domain.data.repository.DerivedDataRepository;
import com.tourswitch.domain.data.repository.DerivedDataRepository.AreaLinkCandidate;
import com.tourswitch.domain.data.repository.DerivedDataRepository.AreaLinkSaveCommand;
import com.tourswitch.domain.data.repository.DerivedDataRepository.CrowdGradeCandidate;
import com.tourswitch.domain.data.repository.DerivedDataRepository.CrowdGradeThreshold;
import com.tourswitch.domain.data.repository.DerivedDataRepository.CrowdGradeUpdate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DerivedDataSyncServiceTest {

    @Mock
    private DerivedDataRepository derivedDataRepository;

    @Mock
    private CrowdLinkRepository crowdLinkRepository;

    @Captor
    private ArgumentCaptor<List<AreaLinkSaveCommand>> areaLinksCaptor;

    @Captor
    private ArgumentCaptor<List<CrowdGradeUpdate>> gradeUpdatesCaptor;

    @Captor
    private ArgumentCaptor<List<CrowdGradeThreshold>> thresholdsCaptor;

    private DerivedDataSyncService service;

    @BeforeEach
    void setUp() {
        PlaceNameMatcher placeNameMatcher = new PlaceNameMatcher();
        CrowdLinkService crowdLinkService = new CrowdLinkService(crowdLinkRepository, placeNameMatcher);
        service = new DerivedDataSyncService(derivedDataRepository, crowdLinkService, placeNameMatcher);
    }

    @Test
    void synchronizeAreaLinks_overlappingAreas_selectsNamedInsideAreaAsPrimary() {
        // Given
        when(derivedDataRepository.findInsideAreaCandidates()).thenReturn(List.of(
                areaCandidate(1L, 10L, "경복궁", "광화문·덕수궁", "POI001", "5000", 0, true),
                areaCandidate(1L, 11L, "경복궁", "경복궁", "POI002", "8000", 0, true)
        ));
        when(derivedDataRepository.findProximityAreaCandidates()).thenReturn(List.of(
                areaCandidate(1L, 12L, "경복궁", "광화문광장", "POI003", "1000", 20, false)
        ));
        when(derivedDataRepository.findManualPrimaryTouristSpotIds()).thenReturn(Set.of());

        // When
        int linkedCount = service.synchronizeAreaLinks();

        // Then
        verify(derivedDataRepository).saveAreaLinks(areaLinksCaptor.capture());
        assertThat(linkedCount).isEqualTo(3);
        assertThat(areaLinksCaptor.getValue())
                .filteredOn(AreaLinkSaveCommand::primary)
                .singleElement()
                .extracting(AreaLinkSaveCommand::areaId)
                .isEqualTo(11L);
    }

    @Test
    void synchronizeCrowdGrades_calculatesContinuousQuartilesAndFourGrades() {
        // Given
        LocalDate forecastDate = LocalDate.of(2026, 9, 10);
        when(derivedDataRepository.findCrowdGradeCandidates()).thenReturn(List.of(
                gradeCandidate(1L, forecastDate, "10"),
                gradeCandidate(2L, forecastDate, "20"),
                gradeCandidate(3L, forecastDate, "30"),
                gradeCandidate(4L, forecastDate, "40")
        ));

        // When
        DerivedDataSyncService.CrowdGradeResult result = service.synchronizeCrowdGrades();

        // Then
        verify(derivedDataRepository).saveCrowdGrades(gradeUpdatesCaptor.capture());
        verify(derivedDataRepository).saveCrowdGradeThresholds(thresholdsCaptor.capture());
        assertThat(result.updatedRows()).isEqualTo(4);
        assertThat(thresholdsCaptor.getValue()).containsExactly(
                new CrowdGradeThreshold(
                        forecastDate,
                        new BigDecimal("17.500"),
                        new BigDecimal("25.000"),
                        new BigDecimal("32.500"),
                        4
                )
        );
        assertThat(gradeUpdatesCaptor.getValue()).extracting(CrowdGradeUpdate::grade)
                .containsExactly("여유", "보통", "붐빔", "매우 붐빔");
        assertThat(gradeUpdatesCaptor.getValue()).extracting(CrowdGradeUpdate::percentile)
                .containsExactly(
                        new BigDecimal("0.000000"),
                        new BigDecimal("0.333333"),
                        new BigDecimal("0.666667"),
                        new BigDecimal("1.000000")
                );
    }

    private AreaLinkCandidate areaCandidate(
            Long touristSpotId,
            Long areaId,
            String touristSpotTitle,
            String areaName,
            String areaCode,
            String areaSize,
            int distanceMeters,
            boolean insideBoundary
    ) {
        return new AreaLinkCandidate(
                touristSpotId,
                touristSpotTitle,
                areaId,
                areaCode,
                areaName,
                new BigDecimal(areaSize),
                distanceMeters,
                insideBoundary
        );
    }

    private CrowdGradeCandidate gradeCandidate(Long id, LocalDate date, String rate) {
        return new CrowdGradeCandidate(id, date, new BigDecimal(rate));
    }
}
