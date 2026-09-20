package com.tourswitch.domain.vote.response;

import com.tourswitch.domain.vote.repository.ParticipantCompletionRow;
import java.time.LocalDateTime;

public record ParticipantStatusResponseDTO(Long memberId, String nickname, boolean completed,
                                            boolean extraCompleted, LocalDateTime completedAt) {

    public static ParticipantStatusResponseDTO from(ParticipantCompletionRow row) {
        return new ParticipantStatusResponseDTO(row.memberId(), row.nickname(), row.completed(),
                row.extraCompleted(), row.completedAt());
    }
}
