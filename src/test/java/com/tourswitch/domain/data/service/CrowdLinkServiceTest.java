package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.data.repository.CrowdForecastLinkCandidate;
import com.tourswitch.domain.data.repository.CrowdLinkRepository;
import com.tourswitch.domain.data.repository.CrowdLinkSaveCommand;
import com.tourswitch.domain.data.repository.TouristSpotLinkCandidate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrowdLinkServiceTest {

    @Mock
    private CrowdLinkRepository crowdLinkRepository;

    @Captor
    private ArgumentCaptor<List<CrowdLinkSaveCommand>> commandsCaptor;

    private CrowdLinkService crowdLinkService;

    @BeforeEach
    void setUp() {
        crowdLinkService = new CrowdLinkService(crowdLinkRepository, new PlaceNameMatcher());
    }

    @Test
    void synchronizeLinks_uniqueSimilarityAboveEighty_createsSimilarLink() {
        // Given
        when(crowdLinkRepository.findActiveTouristSpots()).thenReturn(List.of(
                new TouristSpotLinkCandidate(1L, "11680", "롯데월드타워"),
                new TouristSpotLinkCandidate(2L, "11680", "롯데월드타워동문"),
                new TouristSpotLinkCandidate(3L, "11110", "롯데월드타워점")
        ));
        when(crowdLinkRepository.findCrowdForecastAttractions()).thenReturn(List.of(
                new CrowdForecastLinkCandidate("11680", "롯데월드타워점")
        ));

        // When
        int linkedCount = crowdLinkService.synchronizeLinks();

        // Then
        verify(crowdLinkRepository).saveAll(commandsCaptor.capture());
        assertThat(linkedCount).isEqualTo(1);
        assertThat(commandsCaptor.getValue()).containsExactly(
                new CrowdLinkSaveCommand(1L, "롯데월드타워점", "11680", "SIMILAR")
        );
    }

    @Test
    void synchronizeLinks_normalizedNameExactlyMatches_createsNormalizedLink() {
        // Given
        when(crowdLinkRepository.findActiveTouristSpots()).thenReturn(List.of(
                new TouristSpotLinkCandidate(1L, "11680", "롯데월드 타워")
        ));
        when(crowdLinkRepository.findCrowdForecastAttractions()).thenReturn(List.of(
                new CrowdForecastLinkCandidate("11680", "롯데월드타워")
        ));

        // When
        int linkedCount = crowdLinkService.synchronizeLinks();

        // Then
        verify(crowdLinkRepository).saveAll(commandsCaptor.capture());
        assertThat(linkedCount).isEqualTo(1);
        assertThat(commandsCaptor.getValue()).containsExactly(
                new CrowdLinkSaveCommand(1L, "롯데월드타워", "11680", "NORMALIZED")
        );
    }

    @Test
    void synchronizeLinks_shortNamesNotExactlyEqual_doesNotCreateLink() {
        // Given
        when(crowdLinkRepository.findActiveTouristSpots()).thenReturn(List.of(
                new TouristSpotLinkCandidate(1L, "11680", "서울숲")
        ));
        when(crowdLinkRepository.findCrowdForecastAttractions()).thenReturn(List.of(
                new CrowdForecastLinkCandidate("11680", "서울숩")
        ));

        // When
        int linkedCount = crowdLinkService.synchronizeLinks();

        // Then
        verify(crowdLinkRepository).saveAll(commandsCaptor.capture());
        assertThat(linkedCount).isZero();
        assertThat(commandsCaptor.getValue()).isEmpty();
    }

    @Test
    void synchronizeLinks_bestSimilarityTied_doesNotCreateLink() {
        when(crowdLinkRepository.findActiveTouristSpots()).thenReturn(List.of(
                new TouristSpotLinkCandidate(1L, "11680", "가나다라마바사자"),
                new TouristSpotLinkCandidate(2L, "11680", "가나다라마바사차")
        ));
        when(crowdLinkRepository.findCrowdForecastAttractions()).thenReturn(List.of(
                new CrowdForecastLinkCandidate("11680", "가나다라마바사아")
        ));

        int linkedCount = crowdLinkService.synchronizeLinks();

        verify(crowdLinkRepository).saveAll(commandsCaptor.capture());
        assertThat(linkedCount).isZero();
        assertThat(commandsCaptor.getValue()).isEmpty();
    }
}
