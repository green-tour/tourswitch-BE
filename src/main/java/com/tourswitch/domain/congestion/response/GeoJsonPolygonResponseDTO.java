package com.tourswitch.domain.congestion.response;

import com.tourswitch.global.spatial.model.GeoJsonPolygon;
import java.util.List;

/**
 * 카카오맵 Polygon으로 변환할 수 있는 GeoJSON Polygon 응답을 제공한다.
 */
public record GeoJsonPolygonResponseDTO(
        String type,
        List<List<List<Double>>> coordinates
) {

    /**
     * 범용 공간 값 객체를 API 응답 DTO로 변환한다.
     */
    public static GeoJsonPolygonResponseDTO from(GeoJsonPolygon polygon) {
        return new GeoJsonPolygonResponseDTO(polygon.type(), polygon.coordinates());
    }
}
