package com.tourswitch.global.config.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoConfig {

    @Bean
    public RestClient kakaoRestClient(
        RestClient.Builder restClientBuilder,
        KakaoProperties kakaoProperties
    ) {
        HttpClient httpClient =
            HttpClient.newBuilder()
                .connectTimeout(
                    kakaoProperties.connectTimeout()
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
            kakaoProperties.readTimeout()
        );

        return restClientBuilder
            .clone()
            .requestFactory(requestFactory)
            .build();
    }
}