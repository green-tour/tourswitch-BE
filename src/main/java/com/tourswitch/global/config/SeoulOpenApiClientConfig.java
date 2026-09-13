package com.tourswitch.global.config;

import com.tourswitch.global.client.seoul.SeoulOpenApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SeoulOpenApiClientConfig {

    @Bean
    public RestClient seoulOpenApiRestClient(SeoulOpenApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
