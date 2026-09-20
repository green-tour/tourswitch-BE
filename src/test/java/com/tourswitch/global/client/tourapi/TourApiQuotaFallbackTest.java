package com.tourswitch.global.client.tourapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tourswitch.global.config.TourApiClientConfig;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 호출 한도 초과는 본문뿐 아니라 HTTP 429로도 온다(2026-09-20 실제 호출로 확인).
 * 기본 상태 처리가 켜져 있으면 본문을 못 읽어 한도 초과인지 구분하지 못하고 다음 키로 넘어가지 못한다.
 */
class TourApiQuotaFallbackTest {

    private static final String QUOTA_EXCEEDED_BODY = """
            {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{
              "errMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR",
              "returnAuthMsg":"일일 서비스 요청제한 횟수 초과 에러",
              "returnReasonCode":"22"}}}
            """;

    private static final String ONE_SPOT_BODY = """
            {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
              "body":{"numOfRows":1,"pageNo":1,"totalCount":1,"items":{"item":[
                {"contentid":"1","contenttypeid":"12","title":"테스트관광지",
                 "firstimage":"","mapx":"127.0","mapy":"37.5","lclsSystm2":""}]}}}}
            """;

    private static TourApiProperties propertiesWith(List<String> keys) {
        return new TourApiProperties("https://apis.data.go.kr/B551011", 3000, 5000,
                new TourApiProperties.KorService(keys),
                new TourApiProperties.TatsCnctrRate(keys));
    }

    @Test
    void 한도_초과가_429로_와도_다음_키로_넘어간다() {
        TourApiProperties properties = propertiesWith(List.of("dead-key", "live-key"));
        RestClient.Builder builder = TourApiClientConfig.tourApiRestClientBuilder(properties);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(requestTo(Matchers.containsString("serviceKey=dead-key")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(QUOTA_EXCEEDED_BODY));
        server.expect(requestTo(Matchers.containsString("serviceKey=live-key")))
                .andRespond(withSuccess(ONE_SPOT_BODY, MediaType.APPLICATION_JSON));

        KorServiceClient client = new KorServiceClient(restClient, properties);
        List<TourApiSpotItem> items = client.areaBasedList2("11", "110", 12, null);

        assertThat(items).hasSize(1);
        server.verify();
    }

    @Test
    void 정상_응답이면_첫_키만_쓴다() {
        TourApiProperties properties = propertiesWith(List.of("live-key", "spare-key"));
        RestClient.Builder builder = TourApiClientConfig.tourApiRestClientBuilder(properties);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(requestTo(Matchers.containsString("serviceKey=live-key")))
                .andRespond(withSuccess(ONE_SPOT_BODY, MediaType.APPLICATION_JSON));

        KorServiceClient client = new KorServiceClient(restClient, properties);
        assertThat(client.areaBasedList2("11", "110", 12, null)).hasSize(1);
        server.verify();
    }
}
