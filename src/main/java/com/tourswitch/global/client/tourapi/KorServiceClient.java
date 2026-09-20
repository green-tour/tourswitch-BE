package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 한국관광공사 KorService2(국문 관광정보 서비스) 실시간 호출 클라이언트.
 * 로컬 DB(tourist_spot 등) 캐시를 두지 않고 매 호출마다 API를 부른다(계획 문서 2절 전환 원칙).
 */
@Component
public class KorServiceClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "TourSwitch";
    // TourAPI는 numOfRows 상한이 넉넉하다. 100으로 잘게 끊으면 자치구 하나에 수십 번씩 불러
    // 일일 호출 한도를 빠르게 소진한다(집중률 기준 201회 -> 30회).
    private static final int PAGE_SIZE = 1000;

    private final RestClient tourApiRestClient;
    private final TourApiProperties properties;
    private final TourApiKeyPool keyPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KorServiceClient(RestClient tourApiRestClient, TourApiProperties properties) {
        this.tourApiRestClient = tourApiRestClient;
        this.properties = properties;
        this.keyPool = new TourApiKeyPool("KorService2", properties.korService().serviceKeys());
    }

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
        JsonNode body = get("/KorService2/detailCommon2", Map.of("contentId", contentId));
        return TourApiResponseParser.mapItems(body, TourApiSpotDetail::from).stream().findFirst();
    }

    /**
     * 키가 한도에 걸리면 다음 키로 같은 요청을 다시 보낸다. 페이지 단위로 감싸므로
     * 여러 페이지를 받는 중간에 키가 소진돼도 이미 받은 페이지는 버리지 않는다.
     */
    private JsonNode get(String path, Map<String, String> params) {
        return keyPool.execute(serviceKey -> {
            String raw = tourApiRestClient.get()
                    .uri(buildUri(serviceKey, path, params))
                    .retrieve()
                    .body(String.class);
            return TourApiResponseParser.parseBody(objectMapper, raw);
        });
    }

    private List<TourApiSpotItem> fetchAllPages(String path, Map<String, String> params) {
        List<TourApiSpotItem> all = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            Map<String, String> pageParams = new java.util.LinkedHashMap<>(params);
            pageParams.put("numOfRows", String.valueOf(PAGE_SIZE));
            pageParams.put("pageNo", String.valueOf(pageNo));

            JsonNode body = get(path, pageParams);
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

    private java.net.URI buildUri(String serviceKey, String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl() + path)
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json");
        params.forEach(builder::queryParam);
        return builder.encode().build().toUri();
    }
}
