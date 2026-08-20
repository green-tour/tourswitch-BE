package com.tourswitch.domain.vote.repository;

import java.math.BigDecimal;

public record CandidateDetailRow(
        Long candidateId,
        Long touristSpotId,
        Long keywordId,
        String keywordName,
        String title,
        String imageUrl,
        String overview,
        Integer displayOrder,
        BigDecimal concentrationRateSnapshot,
        String concentrationGradeSnapshot,
        Boolean hasWheelchairAccess,
        Boolean hasStrollerAccess
) {
}
