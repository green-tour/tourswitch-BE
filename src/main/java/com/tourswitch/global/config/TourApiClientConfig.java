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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.resolvedConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.resolvedReadTimeoutMs());

        // baseUrl()을 쓰면 절대경로("/KorService2/...")가 RFC 3986 병합 규칙에 따라
        // baseUrl의 경로("/B551011")를 통째로 대체해버려 최종 경로가 어긋난다(실제 호출로 확인,
        // NO_OPENAPI_SERVICE_ERROR). 그래서 baseUrl은 여기서 설정하지 않고, 각 클라이언트가
        // TourApiProperties.baseUrl()을 직접 이어붙여 완전한 URL을 만든다.
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
