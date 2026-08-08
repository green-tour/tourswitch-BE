package com.tourswitch.domain.vote.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 키워드별 최소 수량 확보(DB설계 7.5절)와 후보 순환(9.3절)을 하나의 순서로 통합한다.
 *
 * 키워드별로 점수순 정렬된 목록을 라운드로빈으로 병합하면, 풀 순서 자체에 키워드 다양성이
 * 매 라운드마다 섞여 들어간다 - 어느 오프셋에서 20개 창을 잘라내도 특정 키워드 후보가
 * 통째로 밀려나지 않는다. 특정 키워드의 후보가 먼저 소진되면 그 라운드부터 자연히
 * 건너뛰어지므로("비례 배분") 별도의 부족분 재분배 로직이 필요 없다.
 */
final class CandidatePoolAssembler {

    private CandidatePoolAssembler() {
    }

    static List<ScoredCandidate> roundRobinMerge(List<Long> keywordOrder,
                                                  Map<Long, List<ScoredCandidate>> rankedByKeyword) {
        List<ScoredCandidate> merged = new ArrayList<>();
        Map<Long, Integer> cursors = new HashMap<>();
        keywordOrder.forEach(keywordId -> cursors.put(keywordId, 0));

        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (Long keywordId : keywordOrder) {
                List<ScoredCandidate> ranked = rankedByKeyword.getOrDefault(keywordId, List.of());
                int cursor = cursors.get(keywordId);
                if (cursor < ranked.size()) {
                    merged.add(ranked.get(cursor));
                    cursors.put(keywordId, cursor + 1);
                    progressed = true;
                }
            }
        }
        return merged;
    }

    /**
     * 9.3절: effective_offset = raw_offset % pool_size. 창 전체를 원형으로 감싸
     * (effective_offset+i) % pool_size를 사용하면 창이 풀 경계를 넘어가는 경우도 자연히 처리된다.
     */
    static List<ScoredCandidate> circularWindow(List<ScoredCandidate> pool, int rawOffset, int maxSize) {
        if (pool.isEmpty()) {
            return List.of();
        }
        int poolSize = pool.size();
        int effectiveOffset = rawOffset % poolSize;
        int windowSize = Math.min(maxSize, poolSize);

        List<ScoredCandidate> window = new ArrayList<>(windowSize);
        for (int i = 0; i < windowSize; i++) {
            window.add(pool.get((effectiveOffset + i) % poolSize));
        }
        return window;
    }
}
