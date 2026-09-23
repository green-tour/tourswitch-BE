package com.tourswitch.domain.place.service;

import com.tourswitch.domain.metadata.model.KeywordCode;
import com.tourswitch.domain.place.exception.PlaceNotFoundException;
import com.tourswitch.domain.place.exception.RegionNotFoundException;
import com.tourswitch.domain.place.model.PlaceCrowdForecast;
import com.tourswitch.domain.place.model.PlaceCrowdForecastQuery;
import com.tourswitch.domain.place.provider.PlaceCrowdForecastProvider;
import com.tourswitch.domain.place.repository.PlaceCrowdNameAliasQueryRepository;
import com.tourswitch.domain.place.repository.PlaceKeywordClassificationQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.domain.place.response.PlaceCrowdForecastResponseDTO;
import com.tourswitch.domain.place.response.PlaceDetailResponseDTO;
import com.tourswitch.domain.place.response.PlaceSummaryResponseDTO;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TourApiSpotDetail;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import com.tourswitch.global.spatial.SpotNameMatcher;
import com.tourswitch.global.response.PageRes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관광지 검색·필터(SRS "관광지 검색·필터", Should). 관광지 마스터를 DB에 적재하지 않고
 * TourAPI를 원본으로 삼는다(TourAPI 실시간전환 계획 문서 2절).
 *
 * 화면이 지역 선택 없이도 카드마다 자치구명과 혼잡도를 요구하므로 SeoulPlaceCache의
 * 자치구별 스냅샷을 우선 사용한다. 스냅샷이 아직 비어 있는 기동 직후에는 기존 경로대로
 * TourAPI를 직접 호출하며, 이때 지역을 지정하지 않으면 자치구 단위로만 조회되는
 * TatsCnctrRateService를 부르지 않아 혼잡도는 비워 둔다(확정되지 않은 값을 노출하지 않는다는
 * 회의록 정책).
 */
@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private static final String SEOUL_AREA_CODE = "11";
    private static final List<Integer> SEARCHABLE_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);
    private final KorServiceClient korServiceClient;
    private final PlaceCrowdForecastProvider placeCrowdForecastProvider;
    private final PlaceCrowdNameAliasQueryRepository placeCrowdNameAliasQueryRepository;
    private final PlaceRegionQueryRepository placeRegionQueryRepository;
    private final PlaceKeywordClassificationQueryRepository placeKeywordClassificationQueryRepository;
    private final SeoulPlaceCache seoulPlaceCache;

    /**
     * 지역, 분류와 현재 혼잡도 필터에 맞는 관광지를 페이지 단위로 반환한다.
     */
    public PageRes<PlaceSummaryResponseDTO> search(Long regionId, List<String> keywordCodes, String congestionLevel,
                                                     int page, int size) {
        PlaceRegionRow region = regionId == null ? null : findRegion(regionId);
        List<String> classificationCodes = resolveClassificationCodes(keywordCodes);

        if (!seoulPlaceCache.isEmpty()) {
            return paginate(searchFromCache(region, classificationCodes, congestionLevel), page, size);
        }

        Map<String, BigDecimal> congestionByName = region == null
                ? Map.of()
                : placeCrowdForecastProvider.findDailyRates(region.legalDongAreaCode(), region.districtCode(),
                        LocalDate.now());

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

    /**
     * 관광지 상세 정보와 오늘부터 최대 30일의 집중률 예측을 반환한다.
     */
    public PlaceDetailResponseDTO getDetail(String contentId, Long regionId) {
        TourApiSpotDetail detail = korServiceClient.detailCommon2(contentId).orElseThrow(PlaceNotFoundException::new);
        // 호출자가 자치구를 넘기지 않아도 캐시에서 찾는다. 그러지 않으면 링크로 바로 들어온
        // 상세 화면에서 자치구와 혼잡도가 비어, 화면이 기본값을 사실처럼 보여준다.
        PlaceRegionRow region = regionId == null ? resolveRegion(detail) : findRegion(regionId);
        LocalDate today = LocalDate.now();
        List<PlaceCrowdForecast> weeklyForecast = region == null
                ? List.of()
                : placeCrowdForecastProvider.findWeeklyForecast(new PlaceCrowdForecastQuery(
                        region.legalDongAreaCode(),
                        region.districtCode(),
                        detail.title(),
                        placeCrowdNameAliasQueryRepository.findAttractionNamesByContentId(detail.contentId()),
                        today
                ));
        BigDecimal concentrationRate = weeklyForecast.stream()
                .filter(forecast -> today.equals(forecast.forecastDate()))
                .map(PlaceCrowdForecast::concentrationRate)
                .findFirst()
                .orElse(null);
        List<PlaceCrowdForecastResponseDTO> weeklyForecastResponse = weeklyForecast.stream()
                .map(forecast -> PlaceCrowdForecastResponseDTO.of(forecast, toGrade(forecast.concentrationRate())))
                .toList();
        return PlaceDetailResponseDTO.of(detail.contentId(), detail.title(),
                region == null ? null : region.districtName(), detail.overview(), detail.firstImageUrl(),
                detail.address(), detail.latitude(), detail.longitude(), toGrade(concentrationRate),
                concentrationRate, weeklyForecastResponse);
    }

    /**
     * 캐시의 contentId를 우선 사용하고, 링크 직접 접근처럼 캐시에 없는 경우 상세 주소에서 자치구를 찾는다.
     */
    private PlaceRegionRow resolveRegion(TourApiSpotDetail detail) {
        PlaceRegionRow cachedRegion = seoulPlaceCache.findDistrictNameByContentId(detail.contentId())
                .flatMap(districtName -> placeRegionQueryRepository.findAll().stream()
                        .filter(row -> districtName.equals(row.districtName()))
                        .findFirst())
                .orElse(null);
        if (cachedRegion != null || detail.address() == null || detail.address().isBlank()) {
            return cachedRegion;
        }
        return placeRegionQueryRepository.findAll().stream()
                .filter(row -> detail.address().contains(row.districtName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 요청 지역 식별자로 장소 검색에 필요한 지역 기준정보를 조회한다.
     */
    private PlaceRegionRow findRegion(Long regionId) {
        return placeRegionQueryRepository.findById(regionId).orElseThrow(RegionNotFoundException::new);
    }

    /**
     * 캐시 스냅샷에서 조회한다. 지역을 지정하지 않아도 자치구명과 혼잡도가 채워지며
     * 카테고리 필터는 보관해 둔 분류코드로 거르므로 TourAPI를 부르지 않는다.
     */
    private List<PlaceSummaryResponseDTO> searchFromCache(PlaceRegionRow region, List<String> classificationCodes,
                                                           String congestionLevel) {
        List<CachedPlace> cached = region == null
                ? seoulPlaceCache.findAll()
                : seoulPlaceCache.findByDistrictName(region.districtName());
        Set<String> wantedCodes = classificationCodes.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return cached.stream()
                .filter(place -> wantedCodes.isEmpty() || wantedCodes.contains(place.classificationLevel2Code()))
                .map(place -> PlaceSummaryResponseDTO.of(place.contentId(), place.title(), place.districtName(),
                        place.imageUrl(), toGrade(place.concentrationRate()), place.concentrationRate()))
                .filter(summary -> congestionLevel == null
                        || congestionLevel.equals(summary.congestion().level()))
                .toList();
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
            String keywordName = KeywordCode.toKeywordName(keywordCode);
            if (keywordName != null) {
                classificationCodes.addAll(placeKeywordClassificationQueryRepository
                        .findClassificationLevel2CodesByKeywordName(keywordName));
            }
        }
        return classificationCodes;
    }

    /**
     * TourAPI 관광지 항목에 현재 지역의 집중률을 결합해 목록 응답으로 변환한다.
     */
    private PlaceSummaryResponseDTO toSummary(TourApiSpotItem item, PlaceRegionRow region,
                                               Map<String, BigDecimal> congestionByName) {
        BigDecimal concentrationRate = congestionByName.get(SpotNameMatcher.key(item.title()));
        return PlaceSummaryResponseDTO.of(item.contentId(), item.title(),
                region == null ? null : region.districtName(), item.firstImageUrl(), toGrade(concentrationRate),
                concentrationRate);
    }

    /**
     * 집중률을 상세·목록 화면에서 공통으로 사용하는 혼잡 단계로 변환한다.
     */
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

    /**
     * 전체 조회 결과에서 요청한 페이지 범위만 잘라 페이지 응답을 생성한다.
     */
    private PageRes<PlaceSummaryResponseDTO> paginate(List<PlaceSummaryResponseDTO> all, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<PlaceSummaryResponseDTO> pageItems = all.subList(fromIndex, toIndex);
        return new PageRes<>(pageItems, all.size(), page, size, toIndex < all.size());
    }
}
