package com.tourswitch.domain.vote.response;

public record CandidateTallyResponseDTO(Long candidateId, String contentId, Integer displayOrder, long voteCount) {

    public static CandidateTallyResponseDTO of(Long candidateId, String contentId, Integer displayOrder,
                                                long voteCount) {
        return new CandidateTallyResponseDTO(candidateId, contentId, displayOrder, voteCount);
    }
}
