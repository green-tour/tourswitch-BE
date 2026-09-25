package com.tourswitch.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tourswitch.domain.course.repository.CourseExtraCandidateQueryRepository;
import com.tourswitch.domain.course.service.CourseGenerationService;
import com.tourswitch.domain.course.service.DraftCourseResetService;
import com.tourswitch.domain.course.service.ExtraVoteService;
import com.tourswitch.domain.vote.exception.VoteAccessDeniedException;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import com.tourswitch.domain.vote.repository.RoomVoteRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 종료된 방을 다시 여는 재투표는 방 전체의 초안 코스를 지우므로 방장만 할 수 있다.
 * 투표 중 자기 완료 표시를 되돌리는 것은 참여자도 계속 할 수 있어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class VoteServiceRevoteTest {

    private static final Long ROOM_ID = 10L;
    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;

    @Mock RoomCandidateRepository roomCandidateRepository;
    @Mock RoomVoteRepository roomVoteRepository;
    @Mock RoomParticipantQueryRepository roomParticipantQueryRepository;
    @Mock TravelRoomStatusQueryRepository travelRoomStatusQueryRepository;
    @Mock CourseGenerationService courseGenerationService;
    @Mock CourseExtraCandidateQueryRepository courseExtraCandidateQueryRepository;
    @Mock ExtraVoteService extraVoteService;
    @Mock DraftCourseResetService draftCourseResetService;

    VoteService service;

    @BeforeEach
    void setUp() {
        service = new VoteService(roomCandidateRepository, roomVoteRepository, roomParticipantQueryRepository,
                travelRoomStatusQueryRepository, courseGenerationService, courseExtraCandidateQueryRepository,
                extraVoteService, draftCourseResetService);
    }

    @Test
    void 참여자는_종료된_방을_다시_열_수_없다() {
        given(roomParticipantQueryRepository.isParticipant(ROOM_ID, GUEST_ID)).willReturn(true);
        given(travelRoomStatusQueryRepository.findStatus(ROOM_ID)).willReturn("CLOSED");
        given(roomParticipantQueryRepository.isHost(ROOM_ID, GUEST_ID)).willReturn(false);

        assertThatThrownBy(() -> service.startRevote(ROOM_ID, GUEST_ID))
                .isInstanceOf(VoteAccessDeniedException.class);

        verify(draftCourseResetService, never()).deleteDraftForRoom(anyLong());
        verify(roomParticipantQueryRepository, never()).resetRoundCompletions(anyLong());
        verify(travelRoomStatusQueryRepository, never()).reopenVotingIfClosed(anyLong());
    }

    @Test
    void 방장은_종료된_방을_다시_연다() {
        given(roomParticipantQueryRepository.isParticipant(ROOM_ID, HOST_ID)).willReturn(true);
        given(travelRoomStatusQueryRepository.findStatus(ROOM_ID)).willReturn("CLOSED", "VOTING");
        given(roomParticipantQueryRepository.isHost(ROOM_ID, HOST_ID)).willReturn(true);
        given(travelRoomStatusQueryRepository.reopenVotingIfClosed(ROOM_ID)).willReturn(true);

        service.startRevote(ROOM_ID, HOST_ID);

        verify(draftCourseResetService).deleteDraftForRoom(ROOM_ID);
        verify(roomParticipantQueryRepository).resetRoundCompletions(ROOM_ID);
        verify(travelRoomStatusQueryRepository).reopenVotingIfClosed(ROOM_ID);
    }

    @Test
    void 투표_중에는_참여자도_자기_완료_표시를_되돌린다() {
        given(roomParticipantQueryRepository.isParticipant(ROOM_ID, GUEST_ID)).willReturn(true);
        given(travelRoomStatusQueryRepository.findStatus(ROOM_ID)).willReturn("VOTING");

        service.startRevote(ROOM_ID, GUEST_ID);

        verify(roomParticipantQueryRepository).updateSelectionCompletion(ROOM_ID, GUEST_ID, false);
        verify(roomParticipantQueryRepository, never()).isHost(anyLong(), anyLong());
        verify(draftCourseResetService, never()).deleteDraftForRoom(anyLong());
    }
}
