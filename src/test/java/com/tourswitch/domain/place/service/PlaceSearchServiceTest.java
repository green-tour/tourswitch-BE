package com.tourswitch.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.place.exception.RegionNotFoundException;
import com.tourswitch.domain.place.repository.PlaceKeywordClassificationQueryRepository;
import com.tourswitch.domain.place.repository.PlaceQueryRepository;
import com.tourswitch.domain.place.repository.PlaceQueryRepository.PlaceDetailRow;
import com.tourswitch.domain.place.repository.PlaceQueryRepository.PlaceForecastRow;
import com.tourswitch.domain.place.repository.PlaceQueryRepository.PlaceSummaryRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

    @Mock
    private PlaceQueryRepository placeQueryRepository;

    @Mock
    private PlaceKeywordClassificationQueryRepository keywordClassificationRepository;

    private PlaceSearchService service;

    @BeforeEach
    void setUp() {
        service = new PlaceSearchService(placeQueryRepository, keywordClassificationRepository);
    }

    @Test
    void 검색은_적재DB의_페이지와_혼잡도를_반환한다() {
        when(placeQueryRepository.regionExists(1L)).thenReturn(true);
        when(placeQueryRepository.countPlaces(1L, List.of())).thenReturn(21L);
        when(placeQueryRepository.findPlaces(1L, List.of(), LocalDate.now(), 0, 20))
                .thenReturn(List.of(new PlaceSummaryRow(
                        "1", "서울숲", "성동구", "image", "여유", new BigDecimal("10")
                )));

        var result = service.search(1L, null, 1, 20);

        assertThat(result.totalCount()).isEqualTo(21);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.items()).singleElement().satisfies(place -> {
            assertThat(place.name()).isEqualTo("서울숲");
            assertThat(place.congestion().level()).isEqualTo("여유");
        });
    }

    @Test
    void 알_수_없는_키워드는_DB를_조회하지_않고_빈_결과를_반환한다() {
        when(placeQueryRepository.regionExists(null)).thenReturn(true);

        var result = service.search(null, List.of("UNKNOWN"), 1, 20);

        assertThat(result.items()).isEmpty();
        verify(placeQueryRepository, never()).countPlaces(null, List.of());
    }

    @Test
    void 존재하지_않는_지역이면_예외가_발생한다() {
        when(placeQueryRepository.regionExists(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.search(999L, null, 1, 20))
                .isInstanceOf(RegionNotFoundException.class);
    }

    @Test
    void 상세는_접근성_예측_실시간_기준시각을_함께_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        PlaceDetailRow detail = new PlaceDetailRow(
                "1", "서울숲", "성동구", null, null, "서울 성동구", 37.5, 127.0,
                true, false, "휠체어 접근 가능", null,
                "여유", new BigDecimal("10"), now,
                "보통", "사람이 다소 있어요", 100, 200, now, now, now
        );
        when(placeQueryRepository.regionExists(null)).thenReturn(true);
        when(placeQueryRepository.findPlace("1", LocalDate.now())).thenReturn(Optional.of(detail));
        when(placeQueryRepository.findForecasts(
                "1", LocalDate.now(), LocalDate.now().plusDays(6)))
                .thenReturn(List.of(new PlaceForecastRow(
                        LocalDate.now(), new BigDecimal("10"), "여유", now
                )));

        var result = service.getDetail("1", null);

        assertThat(result.summary()).isEqualTo("제공 정보 없음");
        assertThat(result.accessibility().wheelchairAccessible()).isTrue();
        assertThat(result.accessibility().strollerDescription()).isEqualTo("제공 정보 없음");
        assertThat(result.forecasts()).hasSize(1);
        assertThat(result.realtimeCongestion().delayed()).isFalse();
        assertThat(result.dataSource()).contains("TourAPI");
    }

    @Test
    void 실시간_수집이_20분을_넘기면_지연으로_표시한다() {
        LocalDateTime staleTime = LocalDateTime.now().minusMinutes(21);
        PlaceDetailRow detail = new PlaceDetailRow(
                "1", "서울숲", "성동구", "소개", null, "주소", 37.5, 127.0,
                false, false, null, null, null, null, null,
                "보통", null, null, null, staleTime, staleTime, staleTime
        );
        when(placeQueryRepository.regionExists(null)).thenReturn(true);
        when(placeQueryRepository.findPlace("1", LocalDate.now())).thenReturn(Optional.of(detail));
        when(placeQueryRepository.findForecasts(
                "1", LocalDate.now(), LocalDate.now().plusDays(6))).thenReturn(List.of());

        assertThat(service.getDetail("1", null).realtimeCongestion().delayed()).isTrue();
    }
}
