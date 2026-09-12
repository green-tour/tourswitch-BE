package com.tourswitch.domain.vote.service;

import com.tourswitch.domain.course.entity.SpotDailyDemand;
import com.tourswitch.domain.course.repository.SpotDailyDemandRepository;
import com.tourswitch.domain.vote.repository.CandidateSpotRow;
import com.tourswitch.domain.vote.repository.RegionDemandBaselineQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DB설계 9.5절: score = 0.6 * crowdEase + 0.4 * demandEase.
 * crowdEase = 1 - (집중률/100) (매칭 없으면 중립값 0.5). TourAPI 실시간전환 이전에는
 * 모집단 대비 백분위(concentrationPercentile)를 썼으나, 실시간 호출은 매 요청마다 모집단
 * 전체를 다시 구해야 해서 TatsCnctrRateService의 원시 집중률(0~100)을 100으로 나눈 값으로
 * 대체한다(TourAPI 실시간전환 계획 문서 7절 관련, CandidateSpotPoolQueryRepository).
 * demandEase = 1 - min(1, participant_count / reference_population_max)
 *              (규모 대리값은 6.2절 규칙 4의 자치구 중앙값 -> 서울 전체 중앙값 폴백을 따른다).
 */
@Component
@RequiredArgsConstructor
public class CandidateScoreCalculator {

    private static final BigDecimal CROWD_WEIGHT = new BigDecimal("0.6");
    private static final BigDecimal DEMAND_WEIGHT = new BigDecimal("0.4");
    private static final BigDecimal NEUTRAL_CROWD_EASE = new BigDecimal("0.5");
    private static final BigDecimal RATE_SCALE = BigDecimal.valueOf(100);
    private static final int SEOUL_WIDE_MEDIAN_POPULATION = 6500;
    private static final int REGION_MEDIAN_MIN_SAMPLE = 3;

    private final RegionDemandBaselineQueryRepository demandBaselineQueryRepository;
    private final SpotDailyDemandRepository spotDailyDemandRepository;

    public BigDecimal crowdEase(CandidateSpotRow row) {
        if (row.concentrationRate() == null) {
            return NEUTRAL_CROWD_EASE;
        }
        BigDecimal ratio = row.concentrationRate().divide(RATE_SCALE, 6, RoundingMode.HALF_UP);
        return BigDecimal.ONE.subtract(ratio.min(BigDecimal.ONE));
    }

    public BigDecimal demandEase(String contentId, Long regionId, double latitude, double longitude,
                                  LocalDate targetDate) {
        int referencePopulationMax = resolveReferencePopulationMax(regionId, latitude, longitude);
        int participantCount = spotDailyDemandRepository.findByContentIdAndTargetDate(contentId, targetDate)
                .map(SpotDailyDemand::getParticipantCount)
                .orElse(0);

        BigDecimal ratio = BigDecimal.valueOf(participantCount)
                .divide(BigDecimal.valueOf(referencePopulationMax), 6, RoundingMode.HALF_UP);
        BigDecimal cappedRatio = ratio.min(BigDecimal.ONE);
        return BigDecimal.ONE.subtract(cappedRatio);
    }

    public BigDecimal score(BigDecimal crowdEase, BigDecimal demandEase) {
        return crowdEase.multiply(CROWD_WEIGHT)
                .add(demandEase.multiply(DEMAND_WEIGHT))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private int resolveReferencePopulationMax(Long regionId, double latitude, double longitude) {
        return demandBaselineQueryRepository.findPrimaryReferencePopulationMax(latitude, longitude)
                .orElseGet(() -> regionOrSeoulWideMedian(regionId));
    }

    private int regionOrSeoulWideMedian(Long regionId) {
        List<Integer> regionPopulations = demandBaselineQueryRepository.findRegionInsideAreaPopulations(regionId);
        if (regionPopulations.size() >= REGION_MEDIAN_MIN_SAMPLE) {
            return median(regionPopulations);
        }
        return SEOUL_WIDE_MEDIAN_POPULATION;
    }

    private int median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2;
        }
        return sorted.get(mid);
    }
}
