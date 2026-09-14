package com.tourswitch.domain.congestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.congestion.repository.MapCongestionQueryRepository;
import com.tourswitch.domain.congestion.repository.MapCongestionQueryRepository.MapCongestionRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapCongestionServiceTest {

    @Mock
    private MapCongestionQueryRepository queryRepository;

    @Test
    void 지도는_영역별_혼잡도_색상_장소_기준시각을_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        when(queryRepository.findLatestAreas()).thenReturn(List.of(
                new MapCongestionRow(
                        1L, "POI001", "강남 MICE 관광특구", "관광특구",
                        new BigDecimal("37.5"), new BigDecimal("127.0"),
                        "여유", "사람이 적어요", 100, 200, now, now,
                        "123", "코엑스", "image", new BigDecimal("37.51"), new BigDecimal("127.01")
                )
        ));

        var response = new MapCongestionService(queryRepository).getCongestionMap();

        assertThat(response.legend()).hasSize(4);
        assertThat(response.delayedAreaCount()).isZero();
        assertThat(response.areas()).singleElement().satisfies(area -> {
            assertThat(area.color()).isEqualTo("#2EBD85");
            assertThat(area.places()).singleElement()
                    .satisfies(place -> assertThat(place.detailPath()).isEqualTo("/api/places/123"));
        });
    }

    @Test
    void 수집값이_없으면_회색과_지연상태를_반환한다() {
        when(queryRepository.findLatestAreas()).thenReturn(List.of(
                new MapCongestionRow(
                        1L, "POI001", "영역", null, null, null,
                        null, null, null, null, null, null,
                        null, null, null, null, null
                )
        ));

        var area = new MapCongestionService(queryRepository).getCongestionMap().areas().getFirst();

        assertThat(area.delayed()).isTrue();
        assertThat(area.color()).isEqualTo("#9E9E9E");
    }
}
