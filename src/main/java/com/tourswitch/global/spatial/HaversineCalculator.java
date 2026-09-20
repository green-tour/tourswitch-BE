package com.tourswitch.global.spatial;

/**
 * 두 좌표 간 직선거리(미터)를 계산한다 - 실제 도로 경로가 아니라 방문 순서 산정용 근사치다
 * (계획 문서 5단계). TourAPI 실시간전환 이전에는 MySQL의 ST_Distance_Sphere를 썼으나, 관광지
 * 좌표를 더 이상 로컬 tourist_spot에 저장하지 않으므로 이미 받아온 좌표로 앱 레이어에서 직접
 * 계산한다(TourAPI 실시간전환 계획 문서 5.3절).
 */
public final class HaversineCalculator {

    private static final int EARTH_RADIUS_METERS = 6_371_000;

    private HaversineCalculator() {
    }

    public static int distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }
}
