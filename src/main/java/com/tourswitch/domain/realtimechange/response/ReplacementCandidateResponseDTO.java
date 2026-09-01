package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.repository.ReplacementCandidateRow;
import java.time.LocalDateTime;
import java.util.List;

public record ReplacementCandidateResponseDTO(
        Long touristSpotId,
        String title,
        String address,
        int distanceMeters,
        List<String> matchedKeywords,
        String crowdGrade,
        LocalDateTime crowdObservedAt
) {

    public static ReplacementCandidateResponseDTO from(ReplacementCandidateRow candidate) {
        return new ReplacementCandidateResponseDTO(
                candidate.touristSpotId(),
                candidate.title(),
                candidate.address(),
                candidate.distanceMeters(),
                candidate.matchedKeywords(),
                candidate.crowdGrade(),
                candidate.crowdObservedAt());
    }
}
