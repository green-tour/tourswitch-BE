package com.tourswitch.global.spatial.model;

import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON Polygon 좌표와 점 포함 판정 기능을 제공하는 범용 공간 값 객체다.
 */
public final class GeoJsonPolygon {

    public static final String TYPE = "Polygon";
    private static final double SEGMENT_EPSILON = 1.0E-10;

    private final List<List<List<Double>>> coordinates;
    private final Polygon polygon;
    private final Bounds bounds;

    private GeoJsonPolygon(List<List<List<Double>>> coordinates, Polygon polygon, Bounds bounds) {
        this.coordinates = coordinates;
        this.polygon = polygon;
        this.bounds = bounds;
    }

    /**
     * 경도·위도 순서의 Polygon 좌표로 공간 값 객체를 생성한다.
     */
    public static GeoJsonPolygon from(List<List<List<Double>>> coordinates) {
        Polygon polygon = Polygon.from(coordinates);
        return new GeoJsonPolygon(coordinates, polygon, Bounds.from(polygon));
    }

    /**
     * GeoJSON geometry 타입을 반환한다.
     */
    public String type() {
        return TYPE;
    }

    /**
     * 변경할 수 없는 GeoJSON Polygon 좌표를 반환한다.
     */
    public List<List<List<Double>>> coordinates() {
        return coordinates;
    }

    /**
     * 주어진 경도·위도가 폴리곤 외곽 내부이면서 내부 구멍 외부인지 확인한다.
     */
    public boolean contains(double longitude, double latitude) {
        return bounds.contains(longitude, latitude) && polygon.contains(longitude, latitude);
    }

    private record Polygon(List<List<Point>> rings) {

        /**
         * GeoJSON ring 좌표를 점 목록으로 변환한다.
         */
        private static Polygon from(List<List<List<Double>>> coordinates) {
            if (coordinates.isEmpty()) {
                throw new IllegalArgumentException("GeoJSON Polygon에 좌표가 없습니다.");
            }
            List<List<Point>> rings = new ArrayList<>();
            for (List<List<Double>> coordinateRing : coordinates) {
                if (coordinateRing.size() < 4) {
                    throw new IllegalArgumentException("GeoJSON Polygon의 ring 좌표가 부족합니다.");
                }
                List<Point> ring = coordinateRing.stream()
                        .map(position -> new Point(position.get(0), position.get(1)))
                        .toList();
                rings.add(ring);
            }
            return new Polygon(List.copyOf(rings));
        }

        /**
         * 외곽 ring과 내부 구멍을 고려해 점 포함 여부를 계산한다.
         */
        private boolean contains(double longitude, double latitude) {
            if (!insideRing(rings.getFirst(), longitude, latitude)) {
                return false;
            }
            return rings.stream()
                    .skip(1)
                    .noneMatch(ring -> insideRing(ring, longitude, latitude));
        }

        /**
         * Ray casting 방식으로 하나의 ring 안에 점이 있는지 계산한다.
         */
        private static boolean insideRing(List<Point> ring, double longitude, double latitude) {
            boolean inside = false;
            for (int current = 0, previous = ring.size() - 1; current < ring.size(); previous = current++) {
                Point currentPoint = ring.get(current);
                Point previousPoint = ring.get(previous);
                if (isOnSegment(previousPoint, currentPoint, longitude, latitude)) {
                    return true;
                }
                boolean crosses = (currentPoint.latitude() > latitude) != (previousPoint.latitude() > latitude)
                        && longitude < (previousPoint.longitude() - currentPoint.longitude())
                        * (latitude - currentPoint.latitude())
                        / (previousPoint.latitude() - currentPoint.latitude())
                        + currentPoint.longitude();
                if (crosses) {
                    inside = !inside;
                }
            }
            return inside;
        }

        /**
         * 점이 ring의 선분 위에 위치하는지 허용 오차를 적용해 확인한다.
         */
        private static boolean isOnSegment(Point start, Point end, double longitude, double latitude) {
            double crossProduct = (latitude - start.latitude()) * (end.longitude() - start.longitude())
                    - (longitude - start.longitude()) * (end.latitude() - start.latitude());
            if (Math.abs(crossProduct) > SEGMENT_EPSILON) {
                return false;
            }
            return longitude >= Math.min(start.longitude(), end.longitude()) - SEGMENT_EPSILON
                    && longitude <= Math.max(start.longitude(), end.longitude()) + SEGMENT_EPSILON
                    && latitude >= Math.min(start.latitude(), end.latitude()) - SEGMENT_EPSILON
                    && latitude <= Math.max(start.latitude(), end.latitude()) + SEGMENT_EPSILON;
        }
    }

    private record Point(double longitude, double latitude) {
    }

    private record Bounds(
            double minimumLongitude,
            double maximumLongitude,
            double minimumLatitude,
            double maximumLatitude
    ) {

        /**
         * 폴리곤 전체 좌표를 감싸는 최소 경계 상자를 계산한다.
         */
        private static Bounds from(Polygon polygon) {
            double minimumLongitude = Double.POSITIVE_INFINITY;
            double maximumLongitude = Double.NEGATIVE_INFINITY;
            double minimumLatitude = Double.POSITIVE_INFINITY;
            double maximumLatitude = Double.NEGATIVE_INFINITY;
            for (List<Point> ring : polygon.rings()) {
                for (Point point : ring) {
                    minimumLongitude = Math.min(minimumLongitude, point.longitude());
                    maximumLongitude = Math.max(maximumLongitude, point.longitude());
                    minimumLatitude = Math.min(minimumLatitude, point.latitude());
                    maximumLatitude = Math.max(maximumLatitude, point.latitude());
                }
            }
            return new Bounds(minimumLongitude, maximumLongitude, minimumLatitude, maximumLatitude);
        }

        /**
         * 점이 최소 경계 상자 안에 있는지 빠르게 확인한다.
         */
        private boolean contains(double longitude, double latitude) {
            return longitude >= minimumLongitude && longitude <= maximumLongitude
                    && latitude >= minimumLatitude && latitude <= maximumLatitude;
        }
    }
}
