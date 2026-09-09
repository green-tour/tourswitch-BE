package com.tourswitch.domain.room.response;

import com.tourswitch.domain.room.entity.TravelRoom;
import com.tourswitch.domain.room.entity.TravelRoomStatus;
import java.time.LocalDate;
import java.util.List;

public record CreateTravelRoomResponse(Long roomId, String inviteToken, String roomName, LocalDate travelDate,
                                       Long regionId, List<Long> keywordIds, int candidateCount,
                                       TravelRoomStatus status) {
    public static CreateTravelRoomResponse from(TravelRoom room, List<Long> keywordIds, int candidateCount) {
        return new CreateTravelRoomResponse(room.getId(), room.getInviteToken(), room.getRoomName(),
                room.getTravelDate(), room.getRegionId(), List.copyOf(keywordIds), candidateCount, room.getStatus());
    }
}
