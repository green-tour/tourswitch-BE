package com.tourswitch.domain.room.response;

import com.tourswitch.domain.room.entity.TravelRoom;

public record JoinRoomResponse(Long roomId, String status) {
    public static JoinRoomResponse from(TravelRoom room) {
        return new JoinRoomResponse(room.getId(), room.getStatus().name());
    }
}
