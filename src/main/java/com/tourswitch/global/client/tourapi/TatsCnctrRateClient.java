package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 한국관광공사 TatsCnctrRateService(관광지 집중률 정보) 실시간 호출 클라이언트.
 * signguCd는 KorService2의 lDongSignguCd(3자리 법정동 코드)와 다른 5자리 시군구 코드를 쓴다
 * (region.district_code, 2026-09-02 실제 호출로 확인 - 계획 문서 11절에서 이미 검증된 사항).
 */
@Component
@RequiredArgsConstructor
public class TatsCnctrRateClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "TourSwitch";
    private static final int PAGE_SIZE = 100;

    private final RestClient tourApiRestClient;
    private final TourApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TourApiCongestionItem> tatsCnctrRatedList(String areaCode, String districtCode) {
        List<TourApiCongestionItem> all = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            Map<String, String> params = Map.of(
                    "areaCd", areaCode,
                    "signguCd", districtCode,
                    "numOfRows", String.valueOf(PAGE_SIZE),
                    "pageNo", String.valueOf(pageNo));
            String raw = tourApiRestClient.get()
                    .uri(buildUri("/TatsCnctrRateService/tatsCnctrRatedList", params))
                    .retrieve()
                    .body(String.class);
            JsonNode body = TourApiResponseParser.parseBody(objectMapper, raw);
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

    private java.net.URI buildUri(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl() + path)
                .queryParam("serviceKey", decodedServiceKey(properties.tatsCnctrRate().serviceKey()))
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
