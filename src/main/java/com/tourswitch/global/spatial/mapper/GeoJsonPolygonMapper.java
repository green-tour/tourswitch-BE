package com.tourswitch.global.spatial.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.global.spatial.model.GeoJsonPolygon;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * GeoJSON 문자열을 경도·위도 순서가 보장된 범용 Polygon 값 객체로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class GeoJsonPolygonMapper {

    private final ObjectMapper objectMapper;

    /**
     * GeoJSON 문자열을 파싱하고 축 순서를 정규화해 Polygon으로 반환한다.
     */
    public GeoJsonPolygon map(String geoJson) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            validatePolygon(root);
            return GeoJsonPolygon.from(coordinates(root.path("coordinates")));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("GeoJSON 문자열을 파싱할 수 없습니다.", exception);
        }
    }

    /**
     * 입력 geometry가 좌표를 가진 Polygon인지 검증한다.
     */
    private void validatePolygon(JsonNode root) {
        if (root == null || !GeoJsonPolygon.TYPE.equals(root.path("type").asText())) {
            throw new IllegalArgumentException("GeoJSON Polygon 형식이 아닙니다.");
        }
        if (!root.path("coordinates").isArray() || root.path("coordinates").isEmpty()) {
            throw new IllegalArgumentException("GeoJSON Polygon 좌표가 없습니다.");
        }
    }

    /**
     * 모든 좌표를 GeoJSON 표준인 경도·위도 순서의 불변 목록으로 변환한다.
     */
    private List<List<List<Double>>> coordinates(JsonNode coordinatesNode) {
        boolean latitudeFirst = isLatitudeFirst(firstPosition(coordinatesNode));
        List<List<List<Double>>> polygon = new ArrayList<>();
        for (JsonNode ringNode : coordinatesNode) {
            List<List<Double>> ring = new ArrayList<>();
            for (JsonNode position : ringNode) {
                validatePosition(position);
                double first = position.path(0).asDouble();
                double second = position.path(1).asDouble();
                ring.add(latitudeFirst ? List.of(second, first) : List.of(first, second));
            }
            polygon.add(List.copyOf(ring));
        }
        return List.copyOf(polygon);
    }

    /**
     * 중첩된 coordinates 배열에서 첫 좌표 쌍을 찾는다.
     */
    private JsonNode firstPosition(JsonNode coordinatesNode) {
        JsonNode position = coordinatesNode;
        while (position.isArray() && !position.isEmpty() && !position.path(0).isNumber()) {
            position = position.path(0);
        }
        validatePosition(position);
        return position;
    }

    /**
     * 좌표가 두 개 이상의 숫자로 구성됐는지 검증한다.
     */
    private void validatePosition(JsonNode position) {
        if (!position.isArray() || position.size() < 2
                || !position.path(0).isNumber() || !position.path(1).isNumber()) {
            throw new IllegalArgumentException("GeoJSON 좌표 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 서울 좌표 범위를 이용해 입력 좌표가 위도·경도 순서인지 확인한다.
     */
    private boolean isLatitudeFirst(JsonNode position) {
        double first = position.path(0).asDouble();
        double second = position.path(1).asDouble();
        return first >= 33.0 && first <= 39.0 && second >= 124.0 && second <= 132.0;
    }
}
