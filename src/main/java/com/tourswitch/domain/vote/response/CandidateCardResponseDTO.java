package com.tourswitch.domain.vote.response;

import com.tourswitch.domain.vote.repository.CandidateDetailRow;

public record CandidateCardResponseDTO(
        Long candidateId,
        Long touristSpotId,
        String title,
        String imageUrl,
        String overview,
        Integer displayOrder,
        String concentrationGrade,
        Boolean hasWheelchairAccess,
        Boolean hasStrollerAccess,
        boolean myVote
) {

    public static CandidateCardResponseDTO of(CandidateDetailRow row, boolean myVote) {
        return new CandidateCardResponseDTO(row.candidateId(), row.touristSpotId(), row.title(), row.imageUrl(),
                row.overview(), row.displayOrder(), row.concentrationGradeSnapshot(), row.hasWheelchairAccess(),
                row.hasStrollerAccess(), myVote);
    }
}
