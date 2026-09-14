package com.tourswitch.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.tourswitch.domain.room.repository.ActiveRoomKeywordRow;
import com.tourswitch.domain.room.repository.ActiveRoomQueryRepository;
import com.tourswitch.domain.room.repository.ActiveRoomRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveRoomServiceTest {
    @Mock ActiveRoomQueryRepository activeRoomQueryRepository;

    @Test
    void 최신_진행_방과_키워드_및_참여_현황을_반환한다() {
        ActiveRoomRow room = new ActiveRoomRow(1L, "성동구 여행", LocalDate.of(2026, 9, 23), 4L,
                "성동구", "VOTING", 2, 1);
        when(activeRoomQueryRepository.findLatestByMemberId(7L)).thenReturn(Optional.of(room));
        when(activeRoomQueryRepository.findKeywords(1L)).thenReturn(List.of(
                new ActiveRoomKeywordRow(3L, "도시공원"), new ActiveRoomKeywordRow(8L, "자연·산")));

        var response = new ActiveRoomService(activeRoomQueryRepository).getActiveRoom(7L);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.keywordIds()).containsExactly(3L, 8L);
        assertThat(response.keywordNames()).containsExactly("도시공원", "자연·산");
        assertThat(response.participantCount()).isEqualTo(2);
        assertThat(response.completedParticipantCount()).isEqualTo(1);
    }

    @Test
    void 진행_중인_방이_없으면_null을_반환한다() {
        when(activeRoomQueryRepository.findLatestByMemberId(7L)).thenReturn(Optional.empty());
        assertThat(new ActiveRoomService(activeRoomQueryRepository).getActiveRoom(7L)).isNull();
        verify(activeRoomQueryRepository, never()).findKeywords(anyLong());
    }
}
