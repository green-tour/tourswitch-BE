package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.DerivedDataRepository;
import com.tourswitch.domain.data.repository.DerivedDataRepository.AreaLinkCandidate;
import com.tourswitch.domain.data.repository.DerivedDataRepository.AreaLinkSaveCommand;
import com.tourswitch.domain.data.repository.DerivedDataRepository.AreaPopulationObservation;
import com.tourswitch.domain.data.repository.DerivedDataRepository.AreaReferencePopulation;
import com.tourswitch.domain.data.repository.DerivedDataRepository.CrowdGradeCandidate;
import com.tourswitch.domain.data.repository.DerivedDataRepository.CrowdGradeThreshold;
import com.tourswitch.domain.data.repository.DerivedDataRepository.CrowdGradeUpdate;
import com.tourswitch.domain.data.repository.DerivedDataRepository.KeywordClassification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DerivedDataSyncService {

    private static final String INSIDE_BOUNDARY = "INSIDE_BOUNDARY";
    private static final String PROXIMITY = "PROXIMITY";
    private static final List<String> KEYWORDS = List.of(
            "전시·박물관", "축제·행사", "도시공원", "역사유적", "레저스포츠", "체험",
            "공연", "자연·산", "종교성지", "골목·거리·둘레길", "랜드마크·전망", "테마파크"
    );
    private static final Map<String, List<String>> KEYWORD_CLASSIFICATIONS = new LinkedHashMap<>();
    private static final List<DuplicatePair> CONFIRMED_DUPLICATES = List.of(
            new DuplicatePair("3428372", "2611568"),
            new DuplicatePair("3458416", "2783317"),
            new DuplicatePair("2946228", "2650046")
    );

    static {
        KEYWORD_CLASSIFICATIONS.put("전시·박물관", List.of("VE07"));
        KEYWORD_CLASSIFICATIONS.put("축제·행사", List.of("EV01", "EV02", "EV03"));
        KEYWORD_CLASSIFICATIONS.put("도시공원", List.of("VE03"));
        KEYWORD_CLASSIFICATIONS.put("역사유적", List.of("HS01", "HS02", "HS04"));
        KEYWORD_CLASSIFICATIONS.put("레저스포츠", List.of("LS01", "LS02", "LS03", "LS04", "VE10", "AC05"));
        KEYWORD_CLASSIFICATIONS.put("체험", List.of("EX01", "EX02", "EX04", "EX05", "EX06", "EX07"));
        KEYWORD_CLASSIFICATIONS.put("공연", List.of("VE06"));
        KEYWORD_CLASSIFICATIONS.put("자연·산", List.of("NA01", "NA02", "NA03", "NA04", "NA05"));
        KEYWORD_CLASSIFICATIONS.put("종교성지", List.of("HS03"));
        KEYWORD_CLASSIFICATIONS.put("골목·거리·둘레길", List.of("VE04", "VE05"));
        KEYWORD_CLASSIFICATIONS.put("랜드마크·전망", List.of("VE01"));
        KEYWORD_CLASSIFICATIONS.put("테마파크", List.of("VE02"));
    }

    private final DerivedDataRepository derivedDataRepository;
    private final CrowdLinkService crowdLinkService;
    private final PlaceNameMatcher placeNameMatcher;

    @Transactional
    public DerivedDataSyncResult synchronizeAll() {
        int duplicateLinks = synchronizeConfirmedDuplicates();
        int keywordLinks = synchronizeKeywordLinks();
        int crowdLinks = crowdLinkService.synchronizeLinks();
        CrowdGradeResult crowdGrades = synchronizeCrowdGrades();
        int areaLinks = synchronizeAreaLinks();
        int referenceAreas = synchronizeReferencePopulationMaximums();
        return new DerivedDataSyncResult(
                duplicateLinks,
                keywordLinks,
                crowdLinks,
                crowdGrades.updatedRows(),
                crowdGrades.thresholds(),
                areaLinks,
                referenceAreas
        );
    }

    @Transactional
    public int synchronizeConfirmedDuplicates() {
        return CONFIRMED_DUPLICATES.stream()
                .mapToInt(pair -> derivedDataRepository.upsertConfirmedDuplicate(
                        pair.duplicateContentId(), pair.canonicalContentId()) > 0 ? 1 : 0)
                .sum();
    }

    @Transactional
    public int synchronizeKeywordLinks() {
        for (int index = 0; index < KEYWORDS.size(); index++) {
            derivedDataRepository.upsertKeyword(KEYWORDS.get(index), index + 1);
        }
        List<KeywordClassification> classifications = KEYWORD_CLASSIFICATIONS.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(code -> new KeywordClassification(entry.getKey(), code)))
                .toList();
        derivedDataRepository.replaceKeywordClassifications(classifications);
        return derivedDataRepository.replaceSpotKeywordLinks();
    }

    @Transactional
    public int synchronizeAreaLinks() {
        List<AreaLinkCandidate> candidates = new ArrayList<>(derivedDataRepository.findInsideAreaCandidates());
        candidates.addAll(derivedDataRepository.findProximityAreaCandidates());
        Set<Long> manualPrimarySpotIds = derivedDataRepository.findManualPrimaryTouristSpotIds();
        Map<Long, List<RankedAreaCandidate>> candidatesBySpot = candidates.stream()
                .map(this::rankAreaCandidate)
                .collect(Collectors.groupingBy(
                        candidate -> candidate.source().touristSpotId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<AreaLinkSaveCommand> commands = new ArrayList<>();
        candidatesBySpot.forEach((touristSpotId, spotCandidates) -> {
            RankedAreaCandidate primary = manualPrimarySpotIds.contains(touristSpotId)
                    ? null
                    : choosePrimaryArea(spotCandidates);
            spotCandidates.stream()
                    .map(candidate -> toSaveCommand(candidate, candidate == primary))
                    .forEach(commands::add);
        });

        derivedDataRepository.deleteAutomaticAreaLinks();
        derivedDataRepository.saveAreaLinks(commands);
        log.info("실시간 영역 링크 갱신 완료: 후보={}건, 연결={}건", candidates.size(), commands.size());
        return commands.size();
    }

    @Transactional
    public CrowdGradeResult synchronizeCrowdGrades() {
        Map<LocalDate, List<CrowdGradeCandidate>> candidatesByDate = derivedDataRepository
                .findCrowdGradeCandidates().stream()
                .collect(Collectors.groupingBy(
                        CrowdGradeCandidate::forecastDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<CrowdGradeUpdate> updates = new ArrayList<>();
        List<CrowdGradeThreshold> thresholds = new ArrayList<>();

        candidatesByDate.forEach((forecastDate, candidates) -> {
            List<CrowdGradeCandidate> sorted = candidates.stream()
                    .sorted(Comparator.comparing(CrowdGradeCandidate::concentrationRate)
                            .thenComparing(CrowdGradeCandidate::forecastId))
                    .toList();
            BigDecimal percentile25 = percentileValue(sorted, new BigDecimal("0.25"));
            BigDecimal percentile50 = percentileValue(sorted, new BigDecimal("0.50"));
            BigDecimal percentile75 = percentileValue(sorted, new BigDecimal("0.75"));
            thresholds.add(new CrowdGradeThreshold(
                    forecastDate, percentile25, percentile50, percentile75, sorted.size()));

            Map<BigDecimal, Integer> firstIndexes = new LinkedHashMap<>();
            for (int index = 0; index < sorted.size(); index++) {
                firstIndexes.putIfAbsent(sorted.get(index).concentrationRate(), index);
            }
            for (CrowdGradeCandidate candidate : sorted) {
                int firstIndex = firstIndexes.get(candidate.concentrationRate());
                BigDecimal percentile = sorted.size() == 1
                        ? BigDecimal.ZERO.setScale(6)
                        : BigDecimal.valueOf(firstIndex)
                        .divide(BigDecimal.valueOf(sorted.size() - 1L), 6, RoundingMode.HALF_UP);
                updates.add(new CrowdGradeUpdate(
                        candidate.forecastId(),
                        percentile,
                        grade(candidate.concentrationRate(), percentile25, percentile50, percentile75)
                ));
            }
        });

        derivedDataRepository.clearCrowdGrades();
        derivedDataRepository.saveCrowdGrades(updates);
        derivedDataRepository.saveCrowdGradeThresholds(thresholds);
        log.info("혼잡도 등급 갱신 완료: 예측={}건, 날짜={}일", updates.size(), thresholds.size());
        return new CrowdGradeResult(updates.size(), thresholds.size());
    }

    @Transactional
    public int synchronizeReferencePopulationMaximums() {
        Map<Long, List<Integer>> valuesByArea = derivedDataRepository.findRecentPopulationMaximums().stream()
                .collect(Collectors.groupingBy(
                        AreaPopulationObservation::areaId,
                        LinkedHashMap::new,
                        Collectors.mapping(AreaPopulationObservation::populationMaximum, Collectors.toList())
                ));
        List<AreaReferencePopulation> references = valuesByArea.entrySet().stream()
                .map(entry -> new AreaReferencePopulation(entry.getKey(), median(entry.getValue())))
                .toList();
        derivedDataRepository.replaceReferencePopulationMaximums(references);
        log.info("실시간 영역 기준 인구 갱신 완료: {}곳", references.size());
        return references.size();
    }

    private RankedAreaCandidate rankAreaCandidate(AreaLinkCandidate candidate) {
        String spotName = placeNameMatcher.normalize(candidate.touristSpotTitle());
        String areaName = placeNameMatcher.normalize(candidate.areaName());
        int priority;
        if (spotName.equals(areaName)) {
            priority = 1;
        } else if (areaName.contains(spotName)) {
            priority = 2;
        } else if (spotName.contains(areaName)) {
            priority = 3;
        } else {
            priority = 4;
        }
        return new RankedAreaCandidate(candidate, priority);
    }

    private RankedAreaCandidate choosePrimaryArea(List<RankedAreaCandidate> candidates) {
        List<RankedAreaCandidate> insideCandidates = candidates.stream()
                .filter(candidate -> candidate.source().insideBoundary())
                .toList();
        List<RankedAreaCandidate> primaryPool = insideCandidates.isEmpty() ? candidates : insideCandidates;
        Comparator<RankedAreaCandidate> comparator = insideCandidates.isEmpty()
                ? Comparator.comparingInt((RankedAreaCandidate candidate) -> candidate.source().distanceMeters())
                .thenComparingInt(RankedAreaCandidate::namePriority)
                : Comparator.comparingInt(RankedAreaCandidate::namePriority);
        return primaryPool.stream()
                .min(comparator
                        .thenComparing(candidate -> candidate.source().areaSizeSquareMeters(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.source().areaCode(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.source().areaId()))
                .orElseThrow();
    }

    private AreaLinkSaveCommand toSaveCommand(RankedAreaCandidate candidate, boolean primary) {
        AreaLinkCandidate source = candidate.source();
        return new AreaLinkSaveCommand(
                source.touristSpotId(),
                source.areaId(),
                source.insideBoundary() ? INSIDE_BOUNDARY : PROXIMITY,
                source.distanceMeters(),
                candidate.namePriority(),
                source.areaSizeSquareMeters(),
                primary
        );
    }

    private BigDecimal percentileValue(List<CrowdGradeCandidate> sorted, BigDecimal percentile) {
        if (sorted.size() == 1) {
            return sorted.getFirst().concentrationRate().setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal position = BigDecimal.valueOf(sorted.size() - 1L).multiply(percentile);
        int lowerIndex = position.setScale(0, RoundingMode.FLOOR).intValueExact();
        int upperIndex = position.setScale(0, RoundingMode.CEILING).intValueExact();
        BigDecimal lower = sorted.get(lowerIndex).concentrationRate();
        BigDecimal upper = sorted.get(upperIndex).concentrationRate();
        BigDecimal fraction = position.subtract(BigDecimal.valueOf(lowerIndex));
        return lower.add(upper.subtract(lower).multiply(fraction)).setScale(3, RoundingMode.HALF_UP);
    }

    private String grade(BigDecimal value, BigDecimal p25, BigDecimal p50, BigDecimal p75) {
        if (value.compareTo(p25) < 0) {
            return "여유";
        }
        if (value.compareTo(p50) < 0) {
            return "보통";
        }
        if (value.compareTo(p75) < 0) {
            return "붐빔";
        }
        return "매우 붐빔";
    }

    private int median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return BigDecimal.valueOf(sorted.get(middle - 1))
                .add(BigDecimal.valueOf(sorted.get(middle)))
                .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private record DuplicatePair(String duplicateContentId, String canonicalContentId) {
    }

    private record RankedAreaCandidate(AreaLinkCandidate source, int namePriority) {
    }

    public record CrowdGradeResult(int updatedRows, int thresholds) {
    }
}
