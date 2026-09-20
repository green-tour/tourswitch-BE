package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 한국관광공사 KorService2(국문 관광정보 서비스) 실시간 호출 클라이언트.
 * 로컬 DB(tourist_spot 등) 캐시를 두지 않고 매 호출마다 API를 부른다(계획 문서 2절 전환 원칙).
 */
@Component
@RequiredArgsConstructor
public class KorServiceClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "TourSwitch";
    // TourAPI는 numOfRows 상한이 넉넉하다. 100으로 잘게 끊으면 자치구 하나에 수십 번씩 불러
    // 일일 호출 한도를 빠르게 소진한다(집중률 기준 201회 -> 30회).
    private static final int PAGE_SIZE = 1000;

    private final RestClient tourApiRestClient;
    private final TourApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TourApiSpotItem> areaBasedList2(String legalDongRegionCode, String legalDongDistrictCode,
                                                 int contentTypeId, String classificationLevel2Code) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("arrange", "C");
        params.put("contentTypeId", String.valueOf(contentTypeId));
        params.put("lDongRegnCd", legalDongRegionCode);
        if (legalDongDistrictCode != null) {
            params.put("lDongSignguCd", legalDongDistrictCode);
        }
        if (classificationLevel2Code != null) {
            params.put("lclsSystm2", classificationLevel2Code);
        }
        return fetchAllPages("/KorService2/areaBasedList2", params);
    }

    public List<TourApiSpotItem> locationBasedList2(double latitude, double longitude, int radiusMeters,
                                                      int contentTypeId) {
        Map<String, String> params = Map.of(
                "arrange", "E",
                "contentTypeId", String.valueOf(contentTypeId),
                "mapX", String.valueOf(longitude),
                "mapY", String.valueOf(latitude),
                "radius", String.valueOf(radiusMeters));
        return fetchAllPages("/KorService2/locationBasedList2", params);
    }

    public Optional<TourApiSpotDetail> detailCommon2(String contentId) {
        String raw = tourApiRestClient.get()
                .uri(buildUri("/KorService2/detailCommon2", Map.of("contentId", contentId)))
                .retrieve()
                .body(String.class);
        JsonNode body = TourApiResponseParser.parseBody(objectMapper, raw);
        return TourApiResponseParser.mapItems(body, TourApiSpotDetail::from).stream().findFirst();
    }

    private List<TourApiSpotItem> fetchAllPages(String path, Map<String, String> params) {
        List<TourApiSpotItem> all = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            Map<String, String> pageParams = new java.util.LinkedHashMap<>(params);
            pageParams.put("numOfRows", String.valueOf(PAGE_SIZE));
            pageParams.put("pageNo", String.valueOf(pageNo));

            String raw = tourApiRestClient.get().uri(buildUri(path, pageParams)).retrieve().body(String.class);
            JsonNode body = TourApiResponseParser.parseBody(objectMapper, raw);
            List<TourApiSpotItem> page = TourApiResponseParser.mapItems(body, TourApiSpotItem::from);
            all.addAll(page);

            int totalCount = TourApiResponseParser.totalCount(body);
            if (page.isEmpty() || (long) pageNo * PAGE_SIZE >= totalCount) {
                break;
            }
            pageNo++;
        }
        return all;
    }

    private java.net.URI buildUri(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl() + path)
                .queryParam("serviceKey", decodedServiceKey(properties.korService().serviceKey()))
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json");
        params.forEach(builder::queryParam);
        return builder.encode().build().toUri();
    }

    private String decodedServiceKey(String serviceKey) {
        return URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
    }
}
