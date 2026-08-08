package com.tourswitch.domain.vote.response;

import com.tourswitch.domain.vote.repository.ParticipantCompletionRow;
import java.time.LocalDateTime;

public record ParticipantStatusResponseDTO(Long memberId, boolean completed, LocalDateTime completedAt) {

    public static ParticipantStatusResponseDTO from(ParticipantCompletionRow row) {
        return new ParticipantStatusResponseDTO(row.memberId(), row.completed(), row.completedAt());
    }
}
