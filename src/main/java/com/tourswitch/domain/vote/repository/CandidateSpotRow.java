package com.tourswitch.domain.vote.repository;

import java.math.BigDecimal;

/**
 * 후보 후보군 조회 결과 한 행 (관광지 x 매칭 키워드 하나). 혼잡도 필드는 매칭이 없으면 null이다.
 */
public record CandidateSpotRow(
        Long touristSpotId,
        Long keywordId,
        BigDecimal concentrationPercentile,
        BigDecimal concentrationRate,
        String concentrationGrade
) {
}
