package com.tourswitch.domain.history.repository;

import java.time.LocalDate;

public record TravelHistoryRow(Long roomId, Long courseId, String roomName, LocalDate travelDate,
                               Long regionId, String regionName, String roomStatus, long participantCount,
                               boolean courseExists, boolean courseConfirmed) {
}
