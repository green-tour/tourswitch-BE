package com.tourswitch.domain.vote.repository;

import java.math.BigDecimal;

/**
 * 후보 후보군 조회 결과 한 행 (관광지 x 매칭 키워드 하나). 혼잡도 필드는 매칭이 없으면 null이다.
 * contentId/title/좌표는 TourAPI areaBasedList2 응답에서, concentrationRate는 TatsCnctrRateService
 * 응답(0~100 사이 원시 집중률)에서 그대로 온다 - 기존에 쓰던 concentrationPercentile(모집단 대비
 * 백분위)은 실시간 호출로는 매 요청마다 모집단 전체를 다시 구해야 해서 이번 전환에서는 원시
 * 집중률을 100으로 나눈 비율로 대체한다(CandidateScoreCalculator.crowdEase).
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
