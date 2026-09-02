package com.tourswitch.domain.history.response;

import com.tourswitch.domain.history.repository.TravelHistoryRow;
import java.time.LocalDate;

public record TravelHistoryItemResponse(Long roomId, Long courseId, String roomName, LocalDate travelDate,
                                        Long regionId, String regionName, String roomStatus, long participantCount,
                                        boolean courseExists, boolean courseConfirmed) {
    public static TravelHistoryItemResponse from(TravelHistoryRow row) {
        return new TravelHistoryItemResponse(row.roomId(), row.courseId(), row.roomName(), row.travelDate(),
                row.regionId(), row.regionName(), row.roomStatus(), row.participantCount(), row.courseExists(),
                row.courseConfirmed());
    }
}
