package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.repository.ReplacementCandidateRow;
import java.time.LocalDateTime;
import java.util.List;

public record ReplacementCandidateResponseDTO(
        String contentId,
        String title,
        String address,
        String imageUrl,
        int distanceMeters,
        List<String> matchedKeywords,
        String crowdGrade,
        LocalDateTime crowdObservedAt
) {

    public static ReplacementCandidateResponseDTO from(ReplacementCandidateRow candidate) {
        return new ReplacementCandidateResponseDTO(
                candidate.contentId(),
                candidate.title(),
                candidate.address(),
                candidate.imageUrl(),
                candidate.distanceMeters(),
                candidate.matchedKeywords(),
                candidate.crowdGrade(),
                candidate.crowdObservedAt());
    }
}
