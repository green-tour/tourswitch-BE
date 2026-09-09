package com.tourswitch.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tourswitch.domain.room.entity.TravelRoom;
import com.tourswitch.domain.room.repository.*;
import com.tourswitch.domain.room.request.CreateTravelRoomRequest;
import com.tourswitch.domain.room.response.CreateTravelRoomResponse;
import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.service.CandidateCompositionPlan;
import com.tourswitch.domain.vote.service.CandidateCompositionService;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TravelRoomServiceTest {
    @Mock TravelRoomRepository travelRoomRepository;
    @Mock RoomKeywordRepository roomKeywordRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;
    @Mock RoomReferenceQueryRepository referenceQueryRepository;
    @Mock CandidateCompositionService candidateCompositionService;
    @Mock RoomCandidateRepository roomCandidateRepository;
    TravelRoomService service;

    @BeforeEach
    void setUp() {
        service = new TravelRoomService(travelRoomRepository, roomKeywordRepository,
                roomParticipantRepository, referenceQueryRepository, candidateCompositionService,
                roomCandidateRepository);
    }

    @Test
    void 방장과_키워드와_후보를_한_흐름으로_생성한다() throws Exception {
        CreateTravelRoomRequest request = new CreateTravelRoomRequest("가을 여행", LocalDate.now().plusDays(1),
                1L, List.of(2L, 4L), 4, true, false, false);
        when(referenceQueryRepository.memberExists(10L)).thenReturn(true);
        when(referenceQueryRepository.regionExists(1L)).thenReturn(true);
        when(referenceQueryRepository.countActiveKeywords(request.keywordIds())).thenReturn(2L);
        when(candidateCompositionService.preparePlan(1L, request.travelDate(), request.keywordIds()))
                .thenReturn(new CandidateCompositionPlan("a".repeat(64), 3));
        when(travelRoomRepository.save(any())).thenAnswer(invocation -> {
            TravelRoom room = invocation.getArgument(0);
            Field id = TravelRoom.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(room, 99L);
            return room;
        });
        when(roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(99L))
                .thenReturn(List.of(mock(RoomCandidate.class), mock(RoomCandidate.class)));

        CreateTravelRoomResponse response = service.createRoom(10L, request);

        assertThat(response.roomId()).isEqualTo(99L);
        assertThat(response.candidateCount()).isEqualTo(2);
        assertThat(response.inviteToken()).hasSize(32);
        verify(roomParticipantRepository).save(argThat(value -> value.isHost() && value.getMemberId().equals(10L)));
        verify(roomKeywordRepository).saveAll(argThat(values -> {
            int count = 0;
            for (Object ignored : values) count++;
            return count == 2;
        }));
        verify(candidateCompositionService).composeCandidates(99L, 1L, request.travelDate(), request.keywordIds(), 3);
    }
}
