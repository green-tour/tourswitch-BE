package com.tourswitch.domain.history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.history.repository.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TravelHistoryServiceTest {
    @Mock TravelHistoryQueryRepository queryRepository;

    @Test
    void 종료된_참여_여행을_페이지로_반환한다() {
        TravelHistoryRow row = new TravelHistoryRow(1L, 2L, "가을 여행", LocalDate.of(2026, 10, 1),
                3L, "종로구", "COURSE_CONFIRMED", 4, true, true);
        when(queryRepository.countHistories(10L)).thenReturn(21L);
        when(queryRepository.findHistories(10L, 0, 20)).thenReturn(List.of(row));

        var result = new TravelHistoryService(queryRepository).getHistories(10L, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().roomName()).isEqualTo("가을 여행");
        assertThat(result.totalCount()).isEqualTo(21);
        assertThat(result.hasNext()).isTrue();
    }
}
