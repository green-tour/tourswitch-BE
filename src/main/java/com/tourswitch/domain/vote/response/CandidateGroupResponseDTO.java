package com.tourswitch.domain.vote.response;

import java.util.List;

public record CandidateGroupResponseDTO(
        Long keywordId,
        String keywordName,
        List<CandidateCardResponseDTO> items
) {
}
