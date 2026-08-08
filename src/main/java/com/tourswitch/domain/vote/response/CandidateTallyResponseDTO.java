package com.tourswitch.domain.vote.response;

public record CandidateTallyResponseDTO(Long candidateId, Long touristSpotId, Integer displayOrder, long voteCount) {

    public static CandidateTallyResponseDTO of(Long candidateId, Long touristSpotId, Integer displayOrder,
                                                long voteCount) {
        return new CandidateTallyResponseDTO(candidateId, touristSpotId, displayOrder, voteCount);
    }
}
