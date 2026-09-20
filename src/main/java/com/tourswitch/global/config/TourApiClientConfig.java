package com.tourswitch.global.config;

import com.tourswitch.global.client.tourapi.TourApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiClientConfig {

    @Bean
    public RestClient tourApiRestClient(TourApiProperties properties) {
        return tourApiRestClientBuilder(properties).build();
    }

    /**
     * 테스트가 같은 설정을 그대로 쓰도록 빌더 단계를 분리해 둔다.
     */
    public static RestClient.Builder tourApiRestClientBuilder(TourApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());

        // baseUrl()을 쓰면 절대경로("/KorService2/...")가 RFC 3986 병합 규칙에 따라
        // baseUrl의 경로("/B551011")를 통째로 대체해버려 최종 경로가 어긋난다(실제 호출로 확인,
        // NO_OPENAPI_SERVICE_ERROR). 그래서 baseUrl은 여기서 설정하지 않고, 각 클라이언트가
        // TourApiProperties.baseUrl()을 직접 이어붙여 완전한 URL을 만든다.
        // data.go.kr은 오류를 본문으로 알려주면서 상태 코드도 같이 올린다(호출 한도 초과는 429).
        // 기본 동작대로 4xx/5xx에서 예외를 던지면 본문을 못 읽어 사유를 알 수 없고,
        // 서비스키 한도 초과인지도 구분하지 못해 다음 키로 넘어갈 수 없다.
        // 상태 코드로는 끊지 않고 본문을 TourApiResponseParser가 해석하게 둔다.
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultStatusHandler(status -> true, (request, response) -> { });
    }
}
