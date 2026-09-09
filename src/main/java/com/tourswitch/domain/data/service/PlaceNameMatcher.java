package com.tourswitch.domain.data.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 외부 데이터 원천마다 다르게 표현된 장소명을 정규화하고 유사도를 계산한다.
 */
@Component
public class PlaceNameMatcher {

    private static final int SHORT_NAME_MAX_LENGTH = 4;
    private static final int MINIMUM_SIMILARITY_PERCENT = 80;
    private static final String NON_ALPHANUMERIC_PATTERN = "[^\\p{L}\\p{N}]";

    public String normalize(String placeName) {
        if (placeName == null || placeName.isBlank()) {
            return "";
        }

        return Normalizer.normalize(placeName, Normalizer.Form.NFKC)
                .replaceAll(NON_ALPHANUMERIC_PATTERN, "")
                .toLowerCase(Locale.ROOT);
    }

    public boolean isSimilarEnough(String normalizedLeftName, String normalizedRightName) {
        if (normalizedLeftName.isBlank() || normalizedRightName.isBlank()) {
            return false;
        }

        int leftLength = normalizedLeftName.codePointCount(0, normalizedLeftName.length());
        int rightLength = normalizedRightName.codePointCount(0, normalizedRightName.length());
        if (Math.min(leftLength, rightLength) <= SHORT_NAME_MAX_LENGTH) {
            return normalizedLeftName.equals(normalizedRightName);
        }

        return calculateSimilarityPercent(normalizedLeftName, normalizedRightName)
                >= MINIMUM_SIMILARITY_PERCENT;
    }

    public int calculateSimilarityPercent(String normalizedLeftName, String normalizedRightName) {
        int[] leftCodePoints = normalizedLeftName.codePoints().toArray();
        int[] rightCodePoints = normalizedRightName.codePoints().toArray();
        int maximumLength = Math.max(leftCodePoints.length, rightCodePoints.length);
        if (maximumLength == 0) {
            return 100;
        }

        int editDistance = calculateLevenshteinDistance(leftCodePoints, rightCodePoints);
        return (maximumLength - editDistance) * 100 / maximumLength;
    }

    private int calculateLevenshteinDistance(int[] leftCodePoints, int[] rightCodePoints) {
        int[] previousDistances = new int[rightCodePoints.length + 1];
        int[] currentDistances = new int[rightCodePoints.length + 1];

        for (int rightIndex = 0; rightIndex <= rightCodePoints.length; rightIndex++) {
            previousDistances[rightIndex] = rightIndex;
        }

        for (int leftIndex = 1; leftIndex <= leftCodePoints.length; leftIndex++) {
            currentDistances[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= rightCodePoints.length; rightIndex++) {
                int replacementCost = leftCodePoints[leftIndex - 1] == rightCodePoints[rightIndex - 1] ? 0 : 1;
                currentDistances[rightIndex] = Math.min(
                        Math.min(
                                currentDistances[rightIndex - 1] + 1,
                                previousDistances[rightIndex] + 1
                        ),
                        previousDistances[rightIndex - 1] + replacementCost
                );
            }

            int[] distanceBuffer = previousDistances;
            previousDistances = currentDistances;
            currentDistances = distanceBuffer;
        }

        return previousDistances[rightCodePoints.length];
    }
}
