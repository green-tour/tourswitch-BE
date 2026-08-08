package com.tourswitch.domain.vote.service;

import java.math.BigDecimal;

public record ScoredCandidate(
        Long touristSpotId,
        Long keywordId,
        BigDecimal score,
        BigDecimal concentrationRate,
        String concentrationGrade
) {
}
