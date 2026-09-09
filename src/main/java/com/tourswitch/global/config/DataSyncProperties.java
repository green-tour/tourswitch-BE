package com.tourswitch.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "data-sync")
public record DataSyncProperties(
        boolean enabled,
        String touristSpotsCron,
        String touristDetailsCron,
        String crowdForecastsCron,
        String seoulRealtimeCron,
        String realtimeStatisticsCron,
        String integrityCheckCron,
        String realtimeAreaCodes
) {

    public List<String> areaCodes() {
        if (realtimeAreaCodes == null || realtimeAreaCodes.isBlank()) {
            return List.of();
        }

        return Arrays.stream(realtimeAreaCodes.split(","))
                .map(String::strip)
                .filter(code -> !code.isBlank())
                .toList();
    }
}
