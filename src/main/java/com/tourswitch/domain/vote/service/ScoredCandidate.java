package com.tourswitch.domain.vote.service;

import java.math.BigDecimal;

public record ScoredCandidate(
        String contentId,
        String title,
        String imageUrl,
        double latitude,
        double longitude,
        Long keywordId,
        BigDecimal score,
        BigDecimal concentrationRate,
        String concentrationGrade
) {
}
