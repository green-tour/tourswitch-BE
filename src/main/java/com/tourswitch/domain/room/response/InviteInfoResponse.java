package com.tourswitch.domain.room.response;

import com.tourswitch.domain.room.entity.TravelRoom;
import java.time.LocalDate;

public record InviteInfoResponse(Long roomId, String roomName, LocalDate travelDate, String status,
                                 boolean valid, boolean requiresAuthentication) {
    public static InviteInfoResponse from(TravelRoom room) {
        return new InviteInfoResponse(room.getId(), room.getRoomName(), room.getTravelDate(),
                room.getStatus().name(), true, true);
    }
}
