package com.tourswitch.domain.congestion.service;

import com.tourswitch.domain.congestion.model.MapAreaAggregation;
import com.tourswitch.domain.congestion.model.MapCongestionSnapshot;
import com.tourswitch.domain.congestion.model.MapPlace;
import com.tourswitch.domain.congestion.provider.MapPlaceProvider;
import com.tourswitch.domain.congestion.repository.MapCongestionQueryRepository;
import com.tourswitch.domain.congestion.response.CongestionLegendResponseDTO;
import com.tourswitch.domain.congestion.response.GeoJsonPolygonResponseDTO;
import com.tourswitch.domain.congestion.response.MapAreaCongestionResponseDTO;
import com.tourswitch.domain.congestion.response.MapCongestionResponseDTO;
import com.tourswitch.domain.congestion.response.MapPlaceResponseDTO;
import com.tourswitch.global.spatial.mapper.GeoJsonPolygonMapper;
import com.tourswitch.global.spatial.model.GeoJsonPolygon;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 서울 영역, 최신 혼잡도와 실시간 관광지를 조합해 지도 데이터 응답을 생성한다.
 */
@Service
@RequiredArgsConstructor
public class MapCongestionService {

    private static final int DELAY_MINUTES = 20;
    private static final String DEFAULT_COLOR = "#9E9E9E";
    private static final List<CongestionLegendResponseDTO> LEGEND = List.of(
            new CongestionLegendResponseDTO("여유", "#2EBD85", 1),
            new CongestionLegendResponseDTO("보통", "#F2C94C", 2),
            new CongestionLegendResponseDTO("약간 붐빔", "#F2994A", 3),
            new CongestionLegendResponseDTO("붐빔", "#EB5757", 4)
    );

    private final MapCongestionQueryRepository queryRepository;
    private final MapPlaceProvider mapPlaceProvider;
    private final GeoJsonPolygonMapper geoJsonPolygonMapper;

    /**
     * 서울 121개 영역의 폴리곤, 최신 혼잡도, 범례와 영역별 관광지를 반환한다.
     */
    public MapCongestionResponseDTO getCongestionMap() {
        LocalDateTime generatedAt = LocalDateTime.now();
        List<MapAreaAggregation> areas = queryRepository.findLatestAreas().stream()
                .map(this::toMapArea)
                .toList();
        linkPlaces(areas, mapPlaceProvider.findSeoulPlaces());

        List<MapAreaCongestionResponseDTO> areaResponses = areas.stream()
                .map(area -> toAreaResponse(area, generatedAt))
                .toList();
        int delayedAreaCount = (int) areaResponses.stream()
                .filter(MapAreaCongestionResponseDTO::delayed)
                .count();
        return new MapCongestionResponseDTO(generatedAt, delayedAreaCount, LEGEND, areaResponses);
    }

    /**
     * DB에서 조회한 GeoJSON 문자열을 공간 판정이 가능한 지도 영역으로 변환한다.
     */
    private MapAreaAggregation toMapArea(MapCongestionSnapshot snapshot) {
        try {
            GeoJsonPolygon boundary = geoJsonPolygonMapper.map(snapshot.boundaryGeoJson());
            return new MapAreaAggregation(snapshot, boundary);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "서울 실시간 영역 경계를 변환하지 못했습니다: " + snapshot.areaCode(),
                    exception
            );
        }
    }

    /**
     * 관광지 좌표가 포함된 첫 서울 영역에 관광지를 연결한다.
     */
    private void linkPlaces(List<MapAreaAggregation> areas, List<MapPlace> places) {
        for (MapPlace place : places) {
            areas.stream()
                    .filter(area -> area.contains(place))
                    .findFirst()
                    .ifPresent(area -> area.addPlace(place));
        }
    }

    /**
     * 집계된 지도 영역을 프론트에 공개할 응답 DTO로 변환한다.
     */
    private MapAreaCongestionResponseDTO toAreaResponse(MapAreaAggregation area, LocalDateTime generatedAt) {
        MapCongestionSnapshot snapshot = area.snapshot();
        boolean delayed = isDelayed(snapshot.collectedAt(), generatedAt);
        List<MapPlaceResponseDTO> places = area.places().stream()
                .map(MapPlaceResponseDTO::from)
                .toList();
        return new MapAreaCongestionResponseDTO(
                snapshot.areaId(), snapshot.areaCode(), snapshot.areaName(), snapshot.category(),
                snapshot.latitude(), snapshot.longitude(), GeoJsonPolygonResponseDTO.from(area.boundary()),
                snapshot.congestionLevel(), color(snapshot.congestionLevel()), snapshot.congestionMessage(),
                snapshot.populationMin(), snapshot.populationMax(), snapshot.observedAt(), snapshot.collectedAt(),
                delayed, places
        );
    }

    /**
     * 최신 혼잡도 수집이 없거나 허용 지연 시간을 넘겼는지 확인한다.
     */
    private boolean isDelayed(LocalDateTime collectedAt, LocalDateTime generatedAt) {
        return collectedAt == null || Duration.between(collectedAt, generatedAt).toMinutes() > DELAY_MINUTES;
    }

    /**
     * 혼잡 단계에 대응하는 지도 표시 색상을 반환한다.
     */
    private String color(String level) {
        if (level == null) {
            return DEFAULT_COLOR;
        }
        return LEGEND.stream()
                .filter(item -> item.level().equals(level))
                .map(CongestionLegendResponseDTO::color)
                .findFirst()
                .orElse(DEFAULT_COLOR);
    }
}
