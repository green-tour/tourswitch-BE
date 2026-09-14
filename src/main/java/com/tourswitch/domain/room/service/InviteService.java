package com.tourswitch.domain.room.service;

import com.tourswitch.domain.room.entity.TravelRoom;
import com.tourswitch.domain.room.entity.TravelRoomStatus;
import com.tourswitch.domain.room.exception.InviteMemberNotFoundException;
import com.tourswitch.domain.room.exception.InviteNotFoundException;
import com.tourswitch.domain.room.exception.InviteParticipationNotAllowedException;
import com.tourswitch.domain.room.repository.RoomParticipantRepository;
import com.tourswitch.domain.room.repository.RoomReferenceQueryRepository;
import com.tourswitch.domain.room.repository.TravelRoomRepository;
import com.tourswitch.domain.room.response.InviteInfoResponse;
import com.tourswitch.domain.room.response.JoinRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InviteService {
    private final TravelRoomRepository travelRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomReferenceQueryRepository referenceQueryRepository;

    @Transactional(readOnly = true)
    public InviteInfoResponse getInvite(String inviteToken) {
        return InviteInfoResponse.from(findRoom(inviteToken));
    }

    @Transactional
    public JoinRoomResponse join(String inviteToken, Long memberId) {
        TravelRoom room = travelRoomRepository.findByInviteTokenForUpdate(inviteToken)
                .orElseThrow(InviteNotFoundException::new);
        if (room.getStatus() != TravelRoomStatus.VOTING) {
            throw new InviteParticipationNotAllowedException();
        }
        if (!referenceQueryRepository.memberExists(memberId)) {
            throw new InviteMemberNotFoundException();
        }
        roomParticipantRepository.insertGuestIfAbsent(room.getId(), memberId);
        return JoinRoomResponse.from(room);
    }

    private TravelRoom findRoom(String inviteToken) {
        return travelRoomRepository.findByInviteToken(inviteToken)
                .orElseThrow(InviteNotFoundException::new);
    }
}
