package com.tourswitch.domain.vote.response;

public record CandidateTallyResponseDTO(Long candidateId, String contentId, String title, String imageUrl,
                                         Integer displayOrder, long voteCount) {

    public static CandidateTallyResponseDTO of(Long candidateId, String contentId, String title, String imageUrl,
                                                Integer displayOrder, long voteCount) {
        return new CandidateTallyResponseDTO(candidateId, contentId, title, imageUrl, displayOrder, voteCount);
    }
}
