package com.tourswitch.domain.room.response;

import com.tourswitch.domain.room.repository.ActiveRoomKeywordRow;
import com.tourswitch.domain.room.repository.ActiveRoomRow;
import java.time.LocalDate;
import java.util.List;

public record ActiveRoomResponse(Long roomId, String roomName, LocalDate travelDate, Long regionId, String regionName,
                                 List<Long> keywordIds, List<String> keywordNames, String status,
                                 long participantCount, long completedParticipantCount) {
    public static ActiveRoomResponse of(ActiveRoomRow room, List<ActiveRoomKeywordRow> keywords) {
        return new ActiveRoomResponse(room.roomId(), room.roomName(), room.travelDate(), room.regionId(),
                room.regionName(), keywords.stream().map(ActiveRoomKeywordRow::keywordId).toList(),
                keywords.stream().map(ActiveRoomKeywordRow::keywordName).toList(), room.status(),
                room.participantCount(), room.completedParticipantCount());
    }
}
