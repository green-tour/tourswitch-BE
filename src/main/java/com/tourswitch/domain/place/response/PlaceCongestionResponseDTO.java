package com.tourswitch.domain.place.response;

import java.math.BigDecimal;

public record PlaceCongestionResponseDTO(String level, BigDecimal rate) {

    public static PlaceCongestionResponseDTO of(String level, BigDecimal rate) {
        return new PlaceCongestionResponseDTO(level, rate);
    }
}
