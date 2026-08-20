package com.tourswitch.domain.vote.response;

import java.util.List;

public record CandidateListResponseDTO(List<CandidateGroupResponseDTO> candidateGroups, int totalCount) {
}
