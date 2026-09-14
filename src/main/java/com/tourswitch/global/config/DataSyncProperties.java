package com.tourswitch.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data-sync")
public record DataSyncProperties(
        boolean enabled,
        String touristSpotsCron,
        String touristDetailsCron,
        String crowdForecastsCron,
        String realtimeStatisticsCron,
        String integrityCheckCron
) {
}
