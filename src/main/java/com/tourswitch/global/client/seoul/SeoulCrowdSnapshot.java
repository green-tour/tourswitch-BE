package com.tourswitch.global.client.seoul;

import java.time.LocalDateTime;

public record SeoulCrowdSnapshot(
        String areaCode,
        String areaName,
        String congestionLevel,
        String congestionMessage,
        Integer populationMin,
        Integer populationMax,
        boolean replacementData,
        LocalDateTime observedAt
) {
}
