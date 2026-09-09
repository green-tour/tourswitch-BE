package com.tourswitch.global.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient.Builder restClientBuilder(ExternalHttpProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.resolvedConnectTimeoutMillis()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.resolvedReadTimeoutMillis()));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
