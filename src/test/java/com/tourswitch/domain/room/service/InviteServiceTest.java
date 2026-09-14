package com.tourswitch.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.tourswitch.domain.room.entity.TravelRoom;
import com.tourswitch.domain.room.entity.TravelRoomStatus;
import com.tourswitch.domain.room.exception.InviteMemberNotFoundException;
import com.tourswitch.domain.room.exception.InviteNotFoundException;
import com.tourswitch.domain.room.exception.InviteParticipationNotAllowedException;
import com.tourswitch.domain.room.repository.RoomParticipantRepository;
import com.tourswitch.domain.room.repository.RoomReferenceQueryRepository;
import com.tourswitch.domain.room.repository.TravelRoomRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {
    @Mock TravelRoomRepository travelRoomRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;
    @Mock RoomReferenceQueryRepository referenceQueryRepository;
    InviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(travelRoomRepository, roomParticipantRepository, referenceQueryRepository);
    }

    @Test
    void 유효한_초대_정보를_반환한다() {
        TravelRoom room = room(1L, TravelRoomStatus.VOTING);
        when(travelRoomRepository.findByInviteToken("token")).thenReturn(Optional.of(room));

        var response = inviteService.getInvite("token");

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("VOTING");
        assertThat(response.valid()).isTrue();
        assertThat(response.requiresAuthentication()).isTrue();
    }

    @Test
    void 참여자는_중복_요청에도_멱등_등록된다() {
        TravelRoom room = room(1L, TravelRoomStatus.VOTING);
        when(travelRoomRepository.findByInviteTokenForUpdate("token")).thenReturn(Optional.of(room));
        when(referenceQueryRepository.memberExists(7L)).thenReturn(true);
        when(roomParticipantRepository.insertGuestIfAbsent(1L, 7L)).thenReturn(0);

        var response = inviteService.join("token", 7L);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("VOTING");
        verify(roomParticipantRepository).insertGuestIfAbsent(1L, 7L);
    }

    @Test
    void 존재하지_않는_초대_토큰은_거부한다() {
        when(travelRoomRepository.findByInviteToken("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inviteService.getInvite("missing"))
                .isInstanceOf(InviteNotFoundException.class);
    }

    @Test
    void 종료된_방에는_참여할_수_없다() {
        TravelRoom room = room(1L, TravelRoomStatus.CLOSED);
        when(travelRoomRepository.findByInviteTokenForUpdate("token"))
                .thenReturn(Optional.of(room));
        assertThatThrownBy(() -> inviteService.join("token", 7L))
                .isInstanceOf(InviteParticipationNotAllowedException.class);
        verifyNoInteractions(referenceQueryRepository, roomParticipantRepository);
    }

    @Test
    void 존재하지_않는_회원은_참여할_수_없다() {
        TravelRoom room = room(1L, TravelRoomStatus.VOTING);
        when(travelRoomRepository.findByInviteTokenForUpdate("token"))
                .thenReturn(Optional.of(room));
        when(referenceQueryRepository.memberExists(999L)).thenReturn(false);
        assertThatThrownBy(() -> inviteService.join("token", 999L))
                .isInstanceOf(InviteMemberNotFoundException.class);
        verifyNoInteractions(roomParticipantRepository);
    }

    private TravelRoom room(Long id, TravelRoomStatus status) {
        TravelRoom room = mock(TravelRoom.class);
        lenient().when(room.getId()).thenReturn(id);
        lenient().when(room.getRoomName()).thenReturn("성동구 여행");
        lenient().when(room.getTravelDate()).thenReturn(LocalDate.of(2026, 9, 20));
        lenient().when(room.getStatus()).thenReturn(status);
        return room;
    }
}
