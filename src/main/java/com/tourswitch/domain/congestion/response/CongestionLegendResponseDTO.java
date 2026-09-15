package com.tourswitch.domain.congestion.response;

/**
 * 프론트가 혼잡도 색상의 의미와 표시 순서를 구성할 수 있도록 범례를 제공한다.
 */
public record CongestionLegendResponseDTO(
        String level,
        String color,
        int displayOrder
) {
}
