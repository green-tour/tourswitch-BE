package com.tourswitch.domain.congestion.model;

import com.tourswitch.global.spatial.model.GeoJsonPolygon;
import java.util.ArrayList;
import java.util.List;

/**
 * 지도 영역의 혼잡도, 폴리곤과 폴리곤 내부 관광지를 응답 생성 전까지 집계한다.
 */
public final class MapAreaAggregation {

    private final MapCongestionSnapshot snapshot;
    private final GeoJsonPolygon boundary;
    private final List<MapPlace> places = new ArrayList<>();

    public MapAreaAggregation(MapCongestionSnapshot snapshot, GeoJsonPolygon boundary) {
        this.snapshot = snapshot;
        this.boundary = boundary;
    }

    /**
     * 영역과 최신 혼잡도 조회 결과를 반환한다.
     */
    public MapCongestionSnapshot snapshot() {
        return snapshot;
    }

    /**
     * 프론트 지도에 전달할 GeoJSON 폴리곤을 반환한다.
     */
    public GeoJsonPolygon boundary() {
        return boundary;
    }

    /**
     * 관광지 좌표가 현재 영역 폴리곤 내부인지 확인한다.
     */
    public boolean contains(MapPlace place) {
        return boundary.contains(place.longitude(), place.latitude());
    }

    /**
     * 현재 영역에 포함된 관광지를 추가한다.
     */
    public void addPlace(MapPlace place) {
        places.add(place);
    }

    /**
     * 현재 영역에 연결된 관광지를 불변 목록으로 반환한다.
     */
    public List<MapPlace> places() {
        return List.copyOf(places);
    }
}
