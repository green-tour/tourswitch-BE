package com.tourswitch.domain.vote.service;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.CandidateSpotPoolQueryRepository;
import com.tourswitch.domain.vote.repository.CandidateSpotRow;
import com.tourswitch.domain.vote.repository.RecommendationConditionCounterQueryRepository;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 방 생성 시점 후보 구성(계획 문서 3단계). TourAPI 실시간전환 이후 이 메서드 자체는
 * @Transactional을 걸지 않는다 - 내부에서 TourAPI를 다건 호출하는데, 외부 API 호출을
 * 트랜잭션 안에 두지 않는다는 B3 규칙 때문이다(TourAPI 실시간전환 계획 문서 8절). API 호출과
 * 점수 계산을 먼저 끝내고, DB 저장(candidate_offset 배정 + room_candidate 저장)만 짧은
 * 트랜잭션으로 묶는다.
 */
@Service
@RequiredArgsConstructor
public class CandidateCompositionService {

    private static final int MAX_CANDIDATES = 20;

    private static final Comparator<ScoredCandidate> SCORE_THEN_CONCENTRATION_COMPARATOR =
            Comparator.comparing(ScoredCandidate::score, Comparator.reverseOrder())
                    .thenComparing(ScoredCandidate::concentrationRate,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private final CandidateSpotPoolQueryRepository candidateSpotPoolQueryRepository;
    private final RecommendationConditionCounterQueryRepository conditionCounterQueryRepository;
    private final CandidateScoreCalculator scoreCalculator;
    private final RoomCandidateRepository roomCandidateRepository;
    private final TransactionTemplate transactionTemplate;

    public void composeCandidates(Long travelRoomId, Long regionId, LocalDate travelDate, List<Long> keywordIds) {
        CandidateCompositionPlan plan = preparePlan(regionId, travelDate, keywordIds);
        composeCandidates(travelRoomId, regionId, travelDate, keywordIds, plan.candidateOffset());
    }

    public CandidateCompositionPlan preparePlan(Long regionId, LocalDate travelDate, List<Long> keywordIds) {
        String conditionKey = RecommendationConditionKeyGenerator.generate(travelDate, regionId, keywordIds);
        int rawOffset = conditionCounterQueryRepository.incrementAndGetRawOffset(conditionKey);
        return new CandidateCompositionPlan(conditionKey, rawOffset);
    }

    public void composeCandidates(Long travelRoomId, Long regionId, LocalDate travelDate, List<Long> keywordIds,
                                  int rawOffset) {
        List<CandidateSpotRow> rows = candidateSpotPoolQueryRepository.findCandidatePool(regionId, keywordIds,
                travelDate);

        List<ScoredCandidate> deduped = dedupeAndScore(rows, regionId, travelDate);
        Map<Long, List<ScoredCandidate>> rankedByKeyword = deduped.stream()
                .collect(Collectors.groupingBy(ScoredCandidate::keywordId));
        rankedByKeyword.replaceAll((keywordId, candidates) -> candidates.stream()
                .sorted(SCORE_THEN_CONCENTRATION_COMPARATOR)
                .toList());

        List<ScoredCandidate> fullPool = CandidatePoolAssembler.roundRobinMerge(keywordIds, rankedByKeyword);
        if (fullPool.isEmpty()) {
            return;
        }

        persist(travelRoomId, fullPool, rawOffset);
    }

    private List<ScoredCandidate> dedupeAndScore(List<CandidateSpotRow> rows, Long regionId, LocalDate travelDate) {
        // 같은 관광지가 여러 선택 키워드에 동시에 매칭될 수 있다. 하나의 키워드에만 귀속시켜
        // room_candidate의 (travel_room_id, content_id) 유니크 제약과 라운드로빈 배분을 단순하게 유지한다.
        Map<String, CandidateSpotRow> firstKeywordPerSpot = new LinkedHashMap<>();
        for (CandidateSpotRow row : rows) {
            firstKeywordPerSpot.putIfAbsent(row.contentId(), row);
        }

        return transactionTemplate.execute(status -> {
            List<ScoredCandidate> scored = new ArrayList<>(firstKeywordPerSpot.size());
            for (CandidateSpotRow row : firstKeywordPerSpot.values()) {
                BigDecimal crowdEase = scoreCalculator.crowdEase(row);
                BigDecimal demandEase = scoreCalculator.demandEase(row.contentId(), regionId, row.latitude(),
                        row.longitude(), travelDate);
                BigDecimal score = scoreCalculator.score(crowdEase, demandEase);
                scored.add(new ScoredCandidate(row.contentId(), row.title(), row.latitude(), row.longitude(),
                        row.keywordId(), score, row.concentrationRate(), row.concentrationGrade()));
            }
            return scored;
        });
    }

    private void persist(Long travelRoomId, List<ScoredCandidate> fullPool, int rawOffset) {
        transactionTemplate.executeWithoutResult(status -> {
            List<ScoredCandidate> window = CandidatePoolAssembler.circularWindow(fullPool, rawOffset,
                    MAX_CANDIDATES);
            List<RoomCandidate> roomCandidates = new ArrayList<>(window.size());
            for (int i = 0; i < window.size(); i++) {
                ScoredCandidate candidate = window.get(i);
                roomCandidates.add(RoomCandidate.create(travelRoomId, candidate.contentId(), candidate.keywordId(),
                        i + 1, candidate.score(), candidate.concentrationRate(), candidate.concentrationGrade(),
                        candidate.title(), candidate.latitude(), candidate.longitude()));
            }
            roomCandidateRepository.saveAll(roomCandidates);
        });
    }
}
