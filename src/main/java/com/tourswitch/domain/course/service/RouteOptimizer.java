package com.tourswitch.domain.course.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 방문 순서 최적화(계획 문서 5단계). course_spot_count는 3~6으로 제한돼 있어(DB CHECK),
 * 가능한 방문 순서 조합이 최대 6!=720가지뿐이다 - 근사 알고리즘 없이 전수 탐색으로
 * 총 이동거리가 최소인 "완전히 최적인" 순서를 찾을 수 있다. 왕복(원점 복귀)이 아니라
 * 첫 지점부터 마지막 지점까지 한 번만 방문하는 경로(열린 경로)를 최소화한다.
 */
final class RouteOptimizer {

    private RouteOptimizer() {
    }

    record Result(List<Long> orderedSpotIds, int totalDistanceMeters) {
    }

    static Result findShortestPath(List<Long> spotIds, Map<Long, Map<Long, Integer>> distanceMatrix) {
        if (spotIds.size() <= 1) {
            return new Result(List.copyOf(spotIds), 0);
        }

        Best best = new Best();
        permute(new ArrayList<>(spotIds), 0, distanceMatrix, best);
        return new Result(best.order, best.totalDistance);
    }

    private static void permute(List<Long> current, int fixedUntil, Map<Long, Map<Long, Integer>> distanceMatrix,
                                 Best best) {
        if (fixedUntil == current.size() - 1) {
            int distance = totalDistance(current, distanceMatrix);
            if (distance < best.totalDistance) {
                best.totalDistance = distance;
                best.order = List.copyOf(current);
            }
            return;
        }
        for (int i = fixedUntil; i < current.size(); i++) {
            swap(current, fixedUntil, i);
            permute(current, fixedUntil + 1, distanceMatrix, best);
            swap(current, fixedUntil, i);
        }
    }

    private static int totalDistance(List<Long> order, Map<Long, Map<Long, Integer>> distanceMatrix) {
        int total = 0;
        for (int i = 0; i < order.size() - 1; i++) {
            total += distanceMatrix.get(order.get(i)).get(order.get(i + 1));
        }
        return total;
    }

    private static void swap(List<Long> list, int i, int j) {
        Long temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    private static final class Best {
        private List<Long> order;
        private int totalDistance = Integer.MAX_VALUE;
    }
}
