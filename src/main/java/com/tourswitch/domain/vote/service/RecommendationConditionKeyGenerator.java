package com.tourswitch.domain.vote.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * travel_room.recommendation_condition_key와 동일한 산출 규칙(계획 문서 7.2절).
 * "여행일|region_id|정렬한 키워드ID목록"을 sha256으로 고정 길이 값으로 만든다.
 */
public final class RecommendationConditionKeyGenerator {

    private RecommendationConditionKeyGenerator() {
    }

    public static String generate(LocalDate travelDate, Long regionId, List<Long> keywordIds) {
        String sortedKeywords = keywordIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String raw = travelDate + "|" + regionId + "|" + sortedKeywords;
        return sha256Hex(raw);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
