package com.tourswitch.domain.place.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlaceForecastResponseDTO(
        LocalDate date,
        BigDecimal concentrationRate,
        String congestionLevel,
        LocalDateTime collectedAt
) {
}
