package com.tourswitch.global.client.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tourapi")
public record TourApiProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        KorService korService,
        TatsCnctrRate tatsCnctrRate
) {

    public record KorService(String serviceKey) {
    }

    public record TatsCnctrRate(String serviceKey) {
    }
}
