package com.tourswitch.domain.vote.repository;

import java.time.LocalDateTime;

public record ParticipantCompletionRow(Long memberId, boolean completed, LocalDateTime completedAt) {
}
