package com.tourswitch.domain.vote.service;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.CandidateSpotPoolQueryRepository;
import com.tourswitch.domain.vote.repository.CandidateSpotRow;
import com.tourswitch.domain.vote.repository.RecommendationConditionCounterQueryRepository;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.support.RecommendationConditionKeyGenerator;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 생성 시점 후보 구성(계획 문서 3단계). 조은혜의 방 생성 트랜잭션 안에서 호출되어
 * 실패 시 방 생성 자체가 함께 롤백되는 것을 전제로 한다(경계 합의 완료).
 */
@Service
@RequiredArgsConstructor
@Transactional
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

    public void composeCandidates(Long travelRoomId, Long regionId, LocalDate travelDate, List<Long> keywordIds) {
        String conditionKey = RecommendationConditionKeyGenerator.generate(travelDate, regionId, keywordIds);
        int rawOffset = conditionCounterQueryRepository.incrementAndGetRawOffset(conditionKey);

        List<ScoredCandidate> deduped = dedupeAndScore(regionId, travelDate, keywordIds);
        Map<Long, List<ScoredCandidate>> rankedByKeyword = deduped.stream()
                .collect(Collectors.groupingBy(ScoredCandidate::keywordId));
        rankedByKeyword.replaceAll((keywordId, candidates) -> candidates.stream()
                .sorted(SCORE_THEN_CONCENTRATION_COMPARATOR)
                .toList());

        List<ScoredCandidate> fullPool = CandidatePoolAssembler.roundRobinMerge(keywordIds, rankedByKeyword);
        if (fullPool.isEmpty()) {
            return;
        }

        List<ScoredCandidate> window = CandidatePoolAssembler.circularWindow(fullPool, rawOffset, MAX_CANDIDATES);
        List<RoomCandidate> roomCandidates = new ArrayList<>(window.size());
        for (int i = 0; i < window.size(); i++) {
            ScoredCandidate candidate = window.get(i);
            roomCandidates.add(RoomCandidate.create(travelRoomId, candidate.touristSpotId(), candidate.keywordId(),
                    i + 1, candidate.score(), candidate.concentrationRate(), candidate.concentrationGrade()));
        }
        roomCandidateRepository.saveAll(roomCandidates);
    }

    private List<ScoredCandidate> dedupeAndScore(Long regionId, LocalDate travelDate, List<Long> keywordIds) {
        List<CandidateSpotRow> rows = candidateSpotPoolQueryRepository.findCandidatePool(regionId, keywordIds,
                travelDate);

        // 같은 관광지가 여러 선택 키워드에 동시에 매칭될 수 있다. 하나의 키워드에만 귀속시켜
        // room_candidate의 (travel_room_id, tourist_spot_id) 유니크 제약과 라운드로빈 배분을 단순하게 유지한다.
        Map<Long, CandidateSpotRow> firstKeywordPerSpot = new LinkedHashMap<>();
        for (CandidateSpotRow row : rows) {
            firstKeywordPerSpot.putIfAbsent(row.touristSpotId(), row);
        }

        List<ScoredCandidate> scored = new ArrayList<>(firstKeywordPerSpot.size());
        for (CandidateSpotRow row : firstKeywordPerSpot.values()) {
            BigDecimal crowdEase = scoreCalculator.crowdEase(row);
            BigDecimal demandEase = scoreCalculator.demandEase(row.touristSpotId(), regionId, travelDate);
            BigDecimal score = scoreCalculator.score(crowdEase, demandEase);
            scored.add(new ScoredCandidate(row.touristSpotId(), row.keywordId(), score, row.concentrationRate(),
                    row.concentrationGrade()));
        }
        return scored;
    }
}
