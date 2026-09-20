package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 한국관광공사 TatsCnctrRateService(관광지 집중률 정보) 실시간 호출 클라이언트.
 * signguCd는 KorService2의 lDongSignguCd(3자리 법정동 코드)와 다른 5자리 시군구 코드를 쓴다
 * (region.district_code, 2026-09-02 실제 호출로 확인 - 계획 문서 11절에서 이미 검증된 사항).
 */
@Component
public class TatsCnctrRateClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "TourSwitch";
    // TourAPI는 numOfRows 상한이 넉넉하다. 100으로 잘게 끊으면 자치구 하나에 수십 번씩 불러
    // 일일 호출 한도를 빠르게 소진한다(집중률 기준 201회 -> 30회).
    private static final int PAGE_SIZE = 1000;

    private final RestClient tourApiRestClient;
    private final TourApiProperties properties;
    private final TourApiKeyPool keyPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TatsCnctrRateClient(RestClient tourApiRestClient, TourApiProperties properties) {
        this.tourApiRestClient = tourApiRestClient;
        this.properties = properties;
        this.keyPool = new TourApiKeyPool("TatsCnctrRateService", properties.tatsCnctrRate().serviceKeys());
    }

    public List<TourApiCongestionItem> tatsCnctrRatedList(String areaCode, String districtCode) {
        List<TourApiCongestionItem> all = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            Map<String, String> params = Map.of(
                    "areaCd", areaCode,
                    "signguCd", districtCode,
                    "numOfRows", String.valueOf(PAGE_SIZE),
                    "pageNo", String.valueOf(pageNo));
            JsonNode body = get("/TatsCnctrRateService/tatsCnctrRatedList", params);
            List<TourApiCongestionItem> page = TourApiResponseParser.mapItems(body, TourApiCongestionItem::from);
            all.addAll(page);

            int totalCount = TourApiResponseParser.totalCount(body);
            if (page.isEmpty() || (long) pageNo * PAGE_SIZE >= totalCount) {
                break;
            }
            pageNo++;
        }
        return all;
    }

    /**
     * 키가 한도에 걸리면 다음 키로 같은 페이지를 다시 받는다. 페이지 단위로 감싸므로
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
