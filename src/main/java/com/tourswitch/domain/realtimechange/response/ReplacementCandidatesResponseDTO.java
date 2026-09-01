package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import com.tourswitch.domain.realtimechange.entity.CourseReplacement;
import com.tourswitch.domain.realtimechange.repository.ReplacementCandidateRow;
import java.util.List;

public record ReplacementCandidatesResponseDTO(
        AdministrativeDongResponseDTO administrativeDong,
        int radiusMeters,
        List<ReplacementCandidateResponseDTO> candidates
) {

    public static ReplacementCandidatesResponseDTO of(AdministrativeDong dong,
                                                       List<ReplacementCandidateRow> candidates) {
        return new ReplacementCandidatesResponseDTO(
                AdministrativeDongResponseDTO.from(dong),
                CourseReplacement.SEARCH_RADIUS_METERS,
                candidates.stream().map(ReplacementCandidateResponseDTO::from).toList());
    }
}
