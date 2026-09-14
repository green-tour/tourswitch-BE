package com.tourswitch.domain.vote.repository;

import java.math.BigDecimal;

/**
 * 후보 후보군 조회 결과 한 행 (관광지 x 매칭 키워드 하나). 혼잡도 필드는 매칭이 없으면 null이다.
 * 관광지·키워드·혼잡도 적재 테이블을 조합한 후보 후보군 조회 결과다.
 */
public record CandidateSpotRow(
        String contentId,
        String title,
        String imageUrl,
        double latitude,
        double longitude,
        Long keywordId,
        BigDecimal concentrationRate,
        String concentrationGrade
) {
}
