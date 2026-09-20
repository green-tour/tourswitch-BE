package com.tourswitch.domain.room.repository;

import java.time.LocalDate;

public record ActiveRoomRow(Long roomId, String roomName, LocalDate travelDate, Long regionId, String regionName,
                            String status, Long hostMemberId, long participantCount,
                            long completedParticipantCount) {
}
