package com.tourswitch.domain.place.service;

import com.tourswitch.domain.place.exception.PlaceNotFoundException;
import com.tourswitch.domain.place.exception.RegionNotFoundException;
import com.tourswitch.domain.place.repository.PlaceKeywordClassificationQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.domain.place.response.PlaceDetailResponseDTO;
import com.tourswitch.domain.place.response.PlaceSummaryResponseDTO;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.client.tourapi.TourApiSpotDetail;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import com.tourswitch.global.response.PageRes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관광지 검색·필터(SRS "관광지 검색·필터", Should). 후보 구성과 동일하게 로컬 캐시 없이
 * TourAPI를 실시간으로 호출한다(TourAPI 실시간전환 계획 문서 2절). 서울 MVP라 지역을 지정하지
 * 않으면 서울(11) 전체를 대상으로 검색하고, 그 경우 자치구 단위로만 조회되는
 * TatsCnctrRateService는 부르지 않아 혼잡도는 비워 둔다(확정되지 않은 값을 노출하지 않는다는
 * 회의록 정책).
 */
@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private static final String SEOUL_AREA_CODE = "11";
    private static final List<Integer> SEARCHABLE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);
    private static final DateTimeFormatter BASE_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KorServiceClient korServiceClient;
    private final TatsCnctrRateClient tatsCnctrRateClient;
    private final PlaceRegionQueryRepository placeRegionQueryRepository;
    private final PlaceKeywordClassificationQueryRepository placeKeywordClassificationQueryRepository;

    public PageRes<PlaceSummaryResponseDTO> search(Long regionId, List<String> keywordCodes, int page, int size) {
        PlaceRegionRow region = regionId == null ? null : findRegion(regionId);
        List<String> classificationCodes = resolveClassificationCodes(keywordCodes);

        Map<String, TourApiCongestionItem> congestionByName = region == null
                ? Map.of()
                : fetchCongestionByName(region, LocalDate.now());

        Map<String, TourApiSpotItem> firstMatchByContentId = new LinkedHashMap<>();
        String areaCode = region == null ? SEOUL_AREA_CODE : region.legalDongAreaCode();
        String districtCode = region == null ? null : region.legalDongDistrictCode();
        for (String classificationCode : classificationCodes) {
            for (int contentTypeId : SEARCHABLE_CONTENT_TYPE_IDS) {
                List<TourApiSpotItem> items = korServiceClient.areaBasedList2(areaCode, districtCode, contentTypeId,
                        classificationCode);
                for (TourApiSpotItem item : items) {
                    firstMatchByContentId.putIfAbsent(item.contentId(), item);
                }
            }
        }

        List<PlaceSummaryResponseDTO> all = firstMatchByContentId.values().stream()
                .map(item -> toSummary(item, region, congestionByName))
                .toList();

        return paginate(all, page, size);
    }

    public PlaceDetailResponseDTO getDetail(String contentId, Long regionId) {
        TourApiSpotDetail detail = korServiceClient.detailCommon2(contentId).orElseThrow(PlaceNotFoundException::new);
        PlaceRegionRow region = regionId == null ? null : findRegion(regionId);
        BigDecimal concentrationRate = null;
        if (region != null) {
            TourApiCongestionItem congestion = fetchCongestionByName(region, LocalDate.now()).get(detail.title());
            concentrationRate = congestion == null ? null : congestion.concentrationRate();
        }
        return PlaceDetailResponseDTO.of(detail.contentId(), detail.title(),
                region == null ? null : region.districtName(), detail.overview(), detail.firstImageUrl(),
                detail.address(), detail.latitude(), detail.longitude(), toGrade(concentrationRate),
                concentrationRate);
    }

    private PlaceRegionRow findRegion(Long regionId) {
        return placeRegionQueryRepository.findById(regionId).orElseThrow(RegionNotFoundException::new);
    }

    /**
     * FE 카테고리 필터 코드(keywordCodes, src/constants/placeCategories.js)를 keyword_classification의
     * lclsSystm2 코드로 변환한다. 코드가 없으면 "전체" 조회이므로 필터 없이 한 번만 순회한다.
     */
    private List<String> resolveClassificationCodes(List<String> keywordCodes) {
        if (keywordCodes == null || keywordCodes.isEmpty()) {
            return java.util.Collections.singletonList(null);
        }
        List<String> classificationCodes = new java.util.ArrayList<>();
        for (String keywordCode : keywordCodes) {
            String keywordName = PlaceCategoryCode.toKeywordName(keywordCode);
            if (keywordName != null) {
                classificationCodes.addAll(placeKeywordClassificationQueryRepository
                        .findClassificationLevel2CodesByKeywordName(keywordName));
            }
        }
        return classificationCodes;
    }

    private Map<String, TourApiCongestionItem> fetchCongestionByName(PlaceRegionRow region, LocalDate targetDate) {
        String targetBaseYmd = targetDate.format(BASE_YMD_FORMAT);
        Map<String, TourApiCongestionItem> byName = new LinkedHashMap<>();
        for (TourApiCongestionItem item : tatsCnctrRateClient.tatsCnctrRatedList(region.legalDongAreaCode(),
                region.districtCode())) {
            if (targetBaseYmd.equals(item.baseYmd())) {
                byName.put(item.touristSpotName(), item);
            }
        }
        return byName;
    }

    private PlaceSummaryResponseDTO toSummary(TourApiSpotItem item, PlaceRegionRow region,
                                               Map<String, TourApiCongestionItem> congestionByName) {
        TourApiCongestionItem congestion = congestionByName.get(item.title());
        BigDecimal concentrationRate = congestion == null ? null : congestion.concentrationRate();
        return PlaceSummaryResponseDTO.of(item.contentId(), item.title(),
                region == null ? null : region.districtName(), item.firstImageUrl(), toGrade(concentrationRate),
                concentrationRate);
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

    private PageRes<PlaceSummaryResponseDTO> paginate(List<PlaceSummaryResponseDTO> all, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<PlaceSummaryResponseDTO> pageItems = all.subList(fromIndex, toIndex);
        return new PageRes<>(pageItems, all.size(), page, size, toIndex < all.size());
    }
}
