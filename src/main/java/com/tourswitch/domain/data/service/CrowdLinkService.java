package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.CrowdForecastLinkCandidate;
import com.tourswitch.domain.data.repository.CrowdLinkRepository;
import com.tourswitch.domain.data.repository.CrowdLinkSaveCommand;
import com.tourswitch.domain.data.repository.TouristSpotLinkCandidate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrowdLinkService {

    private static final String EXACT_MATCH_METHOD = "EXACT";
    private static final String NORMALIZED_MATCH_METHOD = "NORMALIZED";
    private static final String SIMILAR_MATCH_METHOD = "SIMILAR";
    private final CrowdLinkRepository crowdLinkRepository;
    private final PlaceNameMatcher placeNameMatcher;

    /**
     * 같은 자치구에서 원문·정규화 일치를 우선하고, 단일 최고 유사 후보만 자동 링크한다.
     * 수동으로 검토한 링크는 삭제하거나 덮어쓰지 않는다.
     */
    @Transactional
    public int synchronizeLinks() {
        List<TouristSpotLinkCandidate> touristSpots = crowdLinkRepository.findActiveTouristSpots();
        List<CrowdForecastLinkCandidate> attractions = crowdLinkRepository.findCrowdForecastAttractions();
        List<CrowdLinkSaveCommand> linkCommands = createLinkCommands(touristSpots, attractions);
        long reviewCandidateCount = countReviewCandidates(touristSpots, attractions);

        crowdLinkRepository.deleteAutomaticallyMatchedLinks();
        crowdLinkRepository.saveAll(linkCommands);
        crowdLinkRepository.refreshTouristSpotCrowdDataFlags();

        log.info("혼잡도 자동 링크 갱신 완료: 후보 관광지={}건, 원천 장소={}건, 연결={}건, 수동 검토 후보={}건",
                touristSpots.size(), attractions.size(), linkCommands.size(), reviewCandidateCount);
        return linkCommands.size();
    }

    private List<CrowdLinkSaveCommand> createLinkCommands(
            List<TouristSpotLinkCandidate> touristSpots,
            List<CrowdForecastLinkCandidate> attractions
    ) {
        Map<String, List<NormalizedTouristSpot>> touristSpotsByDistrict = touristSpots.stream()
                .map(this::normalizeTouristSpot)
                .filter(touristSpot -> !touristSpot.normalizedTitle().isBlank())
                .collect(Collectors.groupingBy(NormalizedTouristSpot::districtCode));

        return attractions.stream()
                .map(attraction -> createLinkCommand(
                        attraction,
                        touristSpotsByDistrict.getOrDefault(attraction.districtCode(), List.of())
                ))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<CrowdLinkSaveCommand> createLinkCommand(
            CrowdForecastLinkCandidate attraction,
            List<NormalizedTouristSpot> districtTouristSpots
    ) {
        String normalizedAttractionName = placeNameMatcher.normalize(attraction.attractionName());
        if (normalizedAttractionName.isBlank()) {
            return Optional.empty();
        }

        List<NormalizedTouristSpot> normalizedMatches = districtTouristSpots.stream()
                .filter(touristSpot -> touristSpot.normalizedTitle().equals(normalizedAttractionName))
                .toList();
        if (normalizedMatches.isEmpty()) {
            return createSimilarLinkCommand(attraction, districtTouristSpots, normalizedAttractionName);
        }

        List<NormalizedTouristSpot> originalNameMatches = normalizedMatches.stream()
                .filter(touristSpot -> touristSpot.title().equals(attraction.attractionName()))
                .toList();
        NormalizedTouristSpot selected;
        if (originalNameMatches.size() == 1) {
            selected = originalNameMatches.getFirst();
        } else if (normalizedMatches.size() == 1) {
            selected = normalizedMatches.getFirst();
        } else {
            log.warn("혼잡도 자동 링크 보류: districtCode={}, attractionName={}, 동일 정규화 후보={}건",
                    attraction.districtCode(), attraction.attractionName(), normalizedMatches.size());
            return Optional.empty();
        }

        return Optional.of(new CrowdLinkSaveCommand(
                        selected.touristSpotId(),
                        attraction.attractionName(),
                        attraction.districtCode(),
                        selected.title().equals(attraction.attractionName())
                                ? EXACT_MATCH_METHOD
                                : NORMALIZED_MATCH_METHOD
                ));
    }

    private Optional<CrowdLinkSaveCommand> createSimilarLinkCommand(
            CrowdForecastLinkCandidate attraction,
            List<NormalizedTouristSpot> districtTouristSpots,
            String normalizedAttractionName
    ) {
        List<SimilarityCandidate> candidates = districtTouristSpots.stream()
                .filter(touristSpot -> placeNameMatcher.isSimilarEnough(
                        touristSpot.normalizedTitle(), normalizedAttractionName))
                .map(touristSpot -> new SimilarityCandidate(
                        touristSpot,
                        placeNameMatcher.calculateSimilarityPercent(
                                touristSpot.normalizedTitle(), normalizedAttractionName)
                ))
                .sorted(java.util.Comparator.comparingInt(SimilarityCandidate::similarityPercent).reversed())
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        SimilarityCandidate best = candidates.getFirst();
        if (candidates.size() > 1
                && candidates.get(1).similarityPercent() == best.similarityPercent()) {
            log.warn("혼잡도 유사 링크 보류: districtCode={}, attractionName={}, 최고점 동률={}점",
                    attraction.districtCode(), attraction.attractionName(), best.similarityPercent());
            return Optional.empty();
        }
        return Optional.of(new CrowdLinkSaveCommand(
                best.touristSpot().touristSpotId(),
                attraction.attractionName(),
                attraction.districtCode(),
                SIMILAR_MATCH_METHOD
        ));
    }

    private long countReviewCandidates(
            List<TouristSpotLinkCandidate> touristSpots,
            List<CrowdForecastLinkCandidate> attractions
    ) {
        Map<String, List<NormalizedTouristSpot>> touristSpotsByDistrict = touristSpots.stream()
                .map(this::normalizeTouristSpot)
                .collect(Collectors.groupingBy(NormalizedTouristSpot::districtCode));

        return attractions.stream()
                .filter(attraction -> {
                    String normalizedAttraction = placeNameMatcher.normalize(attraction.attractionName());
                    return touristSpotsByDistrict.getOrDefault(attraction.districtCode(), List.of()).stream()
                            .anyMatch(touristSpot -> !touristSpot.normalizedTitle().equals(normalizedAttraction)
                                    && placeNameMatcher.isSimilarEnough(
                                    touristSpot.normalizedTitle(), normalizedAttraction));
                })
                .count();
    }

    private NormalizedTouristSpot normalizeTouristSpot(TouristSpotLinkCandidate touristSpot) {
        return new NormalizedTouristSpot(
                touristSpot.touristSpotId(),
                touristSpot.districtCode(),
                touristSpot.title(),
                placeNameMatcher.normalize(touristSpot.title())
        );
    }

    private record NormalizedTouristSpot(
            Long touristSpotId,
            String districtCode,
            String title,
            String normalizedTitle
    ) {
    }

    private record SimilarityCandidate(NormalizedTouristSpot touristSpot, int similarityPercent) {
    }
}
