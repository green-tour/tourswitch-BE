package com.tourswitch.global.client.tourapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.global.error.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 상태 코드로 끊지 않고 본문을 해석하기로 했으므로, 게이트웨이가 JSON이 아닌 것을 돌려줄 때도
 * 파서가 터지지 않고 오류로 정리해야 한다. 여기서 NPE가 나면 호출부는 원인을 알 수 없는 500을 받는다.
 */
class TourApiResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "<html><body>502 Bad Gateway</body></html>",
            "not json at all",
    })
    void JSON이_아닌_응답은_HTTP_오류로_정리된다(String rawBody) {
        assertThatThrownBy(() -> TourApiResponseParser.parseBody(objectMapper, rawBody))
                .isInstanceOf(TourApiClientException.class)
                .extracting(e -> ((TourApiClientException) e).getCode())
                .isEqualTo(CustomResponseCode.HTTP_ERROR);
    }

    @Test
    void 인증키_한도_초과는_전용_코드로_정리된다() {
        String body = """
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{
                  "errMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR",
                  "returnAuthMsg":"일일 서비스 요청제한 횟수 초과 에러",
                  "returnReasonCode":"22"}}}
                """;

        assertThatThrownBy(() -> TourApiResponseParser.parseBody(objectMapper, body))
                .isInstanceOf(TourApiClientException.class)
                .extracting(e -> ((TourApiClientException) e).getCode())
                .isEqualTo(CustomResponseCode.LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR);
    }

    @Test
    void 정상_응답은_body를_돌려준다() {
        String body = """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                  "body":{"totalCount":7,"items":{"item":[]}}}}
                """;

        assertThat(TourApiResponseParser.totalCount(TourApiResponseParser.parseBody(objectMapper, body)))
                .isEqualTo(7);
    }
}
