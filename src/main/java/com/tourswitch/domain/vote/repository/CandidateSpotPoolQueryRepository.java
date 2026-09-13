package com.tourswitch.domain.vote.repository;

import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 후보 후보군을 TourAPI에서 실시간으로 조회한다(TourAPI 실시간전환 계획 문서 5.1절).
 * 로컬 tourist_spot/spot_keyword_link/spot_crowd_forecast를 더 이상 쓰지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class CandidateSpotPoolQueryRepository {

    private static final List<Integer> CANDIDATE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);
    private static final DateTimeFormatter BASE_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KorServiceClient korServiceClient;
    private final TatsCnctrRateClient tatsCnctrRateClient;
    private final RegionQueryRepository regionQueryRepository;
    private final KeywordClassificationQueryRepository keywordClassificationQueryRepository;

    public List<CandidateSpotRow> findCandidatePool(Long regionId, List<Long> keywordIds, LocalDate travelDate) {
        RegionRow region = regionQueryRepository.findById(regionId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 지역입니다: " + regionId));

        Map<String, TourApiCongestionItem> congestionByName = fetchCongestionByName(region, travelDate);

        // (contentId) 최초 매칭만 유지 - 같은 관광지가 여러 키워드에 매칭돼도 하나의 키워드에만 귀속시킨다.
        Map<String, CandidateSpotRow> firstMatchByContentId = new LinkedHashMap<>();
        for (Long keywordId : keywordIds) {
            for (String classificationLevel2Code : keywordClassificationQueryRepository
                    .findClassificationLevel2CodesByKeywordId(keywordId)) {
                for (int contentTypeId : CANDIDATE_CONTENT_TYPE_IDS) {
                    List<TourApiSpotItem> items = korServiceClient.areaBasedList2(region.legalDongAreaCode(),
                            region.legalDongDistrictCode(), contentTypeId, classificationLevel2Code);
                    for (TourApiSpotItem item : items) {
                        firstMatchByContentId.putIfAbsent(item.contentId(), toRow(item, keywordId, congestionByName));
                    }
                }
            }
        }
        return List.copyOf(firstMatchByContentId.values());
    }

    private Map<String, TourApiCongestionItem> fetchCongestionByName(RegionRow region, LocalDate travelDate) {
        String targetBaseYmd = travelDate.format(BASE_YMD_FORMAT);
        Map<String, TourApiCongestionItem> byName = new LinkedHashMap<>();
        for (TourApiCongestionItem item : tatsCnctrRateClient.tatsCnctrRatedList(region.legalDongAreaCode(),
                region.districtCode())) {
            if (targetBaseYmd.equals(item.baseYmd())) {
                byName.put(item.touristSpotName(), item);
            }
        }
        return byName;
    }

    private CandidateSpotRow toRow(TourApiSpotItem item, Long keywordId,
                                    Map<String, TourApiCongestionItem> congestionByName) {
        TourApiCongestionItem congestion = congestionByName.get(item.title());
        BigDecimal concentrationRate = congestion == null ? null : congestion.concentrationRate();
        String concentrationGrade = toGrade(concentrationRate);
        return new CandidateSpotRow(item.contentId(), item.title(), item.firstImageUrl(), item.latitude(),
                item.longitude(), keywordId, concentrationRate, concentrationGrade);
    }

    private String toGrade(BigDecimal concentrationRate) {
        if (concentrationRate == null) {
            return null;
        }
        double rate = concentrationRate.doubleValue();
        if (rate < 25) {
            return "여유";
        }
        if (rate < 50) {
            return "보통";
        }
        if (rate < 75) {
            return "약간 붐빔";
        }
        return "붐빔";
    }
}
