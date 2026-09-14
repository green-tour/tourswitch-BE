package com.tourswitch.domain.congestion.response;

import java.math.BigDecimal;

public record MapPlaceResponseDTO(
        String contentId,
        String name,
        String imageUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        String detailPath
) {
}
