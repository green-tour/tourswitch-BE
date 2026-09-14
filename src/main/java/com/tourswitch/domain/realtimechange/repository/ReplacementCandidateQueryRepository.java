package com.tourswitch.domain.realtimechange.repository;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import com.tourswitch.domain.vote.repository.KeywordClassificationQueryRepository;
import com.tourswitch.domain.vote.repository.RegionQueryRepository;
import com.tourswitch.domain.vote.repository.RegionRow;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/** 선택 행정동의 대표 좌표 기준 3km 후보를 TourAPI에서 실시간 조회한다. */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ReplacementCandidateQueryRepository {

    private static final int SEARCH_RADIUS_METERS = 3_000;
    private static final List<Integer> ATTRACTION_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);
    private static final DateTimeFormatter BASE_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KorServiceClient korServiceClient;
    private final TatsCnctrRateClient tatsCnctrRateClient;
    private final RegionQueryRepository regionQueryRepository;
    private final KeywordClassificationQueryRepository keywordClassificationQueryRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final SeoulRealtimeCrowdQueryRepository seoulRealtimeCrowdQueryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<ReplacementCandidateRow> findCandidates(Course course, AdministrativeDong dong, int limit) {
        Map<String, List<String>> keywordNamesByClassification = findKeywordNamesByClassification(
                course.getTravelRoomId());
        if (keywordNamesByClassification.isEmpty()) {
            return List.of();
        }
        Set<String> existingContentIds = new HashSet<>(courseSpotRepository
                .findByCourseIdOrderByVisitOrderAsc(course.getId()).stream()
                .map(spot -> spot.getContentId()).toList());
        Map<String, TourApiCongestionItem> congestionByName = fetchCongestionByName(
                dong.getRegionId(), course.getTravelDate().format(BASE_YMD_FORMAT));

        Map<String, ReplacementCandidateRow> candidatesByContentId = new LinkedHashMap<>();
        for (int contentTypeId : ATTRACTION_CONTENT_TYPE_IDS) {
            for (TourApiSpotItem spot : korServiceClient.locationBasedList2(
                    dong.getCenterLatitude().doubleValue(), dong.getCenterLongitude().doubleValue(),
                    SEARCH_RADIUS_METERS, contentTypeId)) {
                List<String> matchedKeywords = keywordNamesByClassification.get(spot.classificationLevel2Code());
                if (matchedKeywords == null || existingContentIds.contains(spot.contentId())) {
                    continue;
                }
                TourApiCongestionItem congestion = congestionByName.get(spot.title());
                Optional<SeoulRealtimeCrowdRow> seoulCrowd = seoulRealtimeCrowdQueryRepository.findLatest(
                        spot.latitude(), spot.longitude());
                candidatesByContentId.putIfAbsent(spot.contentId(), new ReplacementCandidateRow(
                        spot.contentId(), spot.title(), spot.address(), spot.firstImageUrl(), distanceOf(spot), matchedKeywords,
                        seoulCrowd.map(SeoulRealtimeCrowdRow::congestionLevel)
                                .orElseGet(() -> congestion == null ? null : toGrade(congestion.concentrationRate())),
                        seoulCrowd.map(SeoulRealtimeCrowdRow::observedAt).orElse(null)));
            }
        }
        return candidatesByContentId.values().stream()
                .sorted(Comparator.comparingInt(this::crowdRank)
                        .thenComparing(Comparator.comparingInt(
                                (ReplacementCandidateRow row) -> row.matchedKeywords().size()).reversed())
                        .thenComparingInt(ReplacementCandidateRow::distanceMeters)
                        .thenComparing(ReplacementCandidateRow::contentId))
                .limit(limit).toList();
    }

    public Optional<ReplacementCandidateRow> findEligibleCandidate(Course course, AdministrativeDong dong,
                                                                    String contentId) {
        return findCandidates(course, dong, 50).stream()
                .filter(candidate -> candidate.contentId().equals(contentId)).findFirst();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> findKeywordNamesByClassification(Long travelRoomId) {
        List<Object[]> keywords = entityManager.createNativeQuery("""
                SELECT k.id, k.keyword_name
                FROM room_keyword rk
                JOIN keyword k ON k.id = rk.keyword_id AND k.is_active = TRUE
                WHERE rk.travel_room_id = :travelRoomId
                ORDER BY k.display_order, k.id
                """).setParameter("travelRoomId", travelRoomId).getResultList();
        Map<String, List<String>> result = new HashMap<>();
        for (Object[] keyword : keywords) {
            Long keywordId = ((Number) keyword[0]).longValue();
            String keywordName = (String) keyword[1];
            for (String code : keywordClassificationQueryRepository
                    .findClassificationLevel2CodesByKeywordId(keywordId)) {
                result.computeIfAbsent(code, ignored -> new ArrayList<>()).add(keywordName);
            }
        }
        result.replaceAll((code, names) -> List.copyOf(names));
        return result;
    }

    private Map<String, TourApiCongestionItem> fetchCongestionByName(Long regionId, String targetBaseYmd) {
        RegionRow region = regionQueryRepository.findById(regionId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 지역입니다: " + regionId));
        Map<String, TourApiCongestionItem> result = new HashMap<>();
        try {
            for (TourApiCongestionItem item : tatsCnctrRateClient.tatsCnctrRatedList(
                    region.legalDongAreaCode(), region.districtCode())) {
                if (targetBaseYmd.equals(item.baseYmd())) result.put(item.touristSpotName(), item);
            }
        } catch (RuntimeException exception) {
            // 집중률은 보조 점수다. 장애 시 후보 조회 자체를 중단하지 않고 키워드·거리로 추천한다.
            log.warn("관광지 집중률 조회에 실패해 혼잡도 없이 대체 후보를 구성합니다. regionId={}", regionId,
                    exception);
        }
        return result;
    }

    private int distanceOf(TourApiSpotItem spot) {
        return spot.distanceMeters() == null ? Integer.MAX_VALUE : (int) Math.round(spot.distanceMeters());
    }

    private int crowdRank(ReplacementCandidateRow row) {
        return switch (row.crowdGrade() == null ? "" : row.crowdGrade()) {
            case "여유" -> 1; case "보통" -> 2; case "약간 붐빔" -> 3; case "붐빔" -> 4; default -> 5;
        };
    }

    private String toGrade(BigDecimal rate) {
        if (rate.doubleValue() < 25) return "여유";
        if (rate.doubleValue() < 50) return "보통";
        if (rate.doubleValue() < 75) return "약간 붐빔";
        return "붐빔";
    }
}
