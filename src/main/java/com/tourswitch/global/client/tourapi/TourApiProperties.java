package com.tourswitch.global.client.tourapi;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tourapi")
public record TourApiProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        KorService korService,
        TatsCnctrRate tatsCnctrRate
) {

    /** 서비스키는 여러 개를 쉼표로 이어 준다. 앞의 키가 한도에 걸리면 뒤의 키로 넘어간다. */
    public record KorService(List<String> serviceKeys) {
    }

    public record TatsCnctrRate(List<String> serviceKeys) {
    }
}
