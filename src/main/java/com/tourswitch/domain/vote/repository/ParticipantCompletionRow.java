package com.tourswitch.domain.vote.repository;

import java.time.LocalDateTime;

/**
 * extraCompleted는 추가 투표 라운드의 완료 여부다. 라운드가 둘이라 화면이 어디로 보낼지
 * 판단하려면 두 값을 구분해서 봐야 한다.
 */
public record ParticipantCompletionRow(Long memberId, boolean completed, boolean extraCompleted,
                                        LocalDateTime completedAt) {
}
