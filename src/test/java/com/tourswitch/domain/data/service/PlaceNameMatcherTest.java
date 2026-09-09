package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceNameMatcherTest {

    private PlaceNameMatcher placeNameMatcher;

    @BeforeEach
    void setUp() {
        placeNameMatcher = new PlaceNameMatcher();
    }

    @Test
    void normalize_nameWithWhitespaceAndSymbols_removesNonAlphanumericCharacters() {
        // Given
        String placeName = "  서울, 숲 (야간)!  ";

        // When
        String normalizedName = placeNameMatcher.normalize(placeName);

        // Then
        assertThat(normalizedName).isEqualTo("서울숲야간");
    }

    @Test
    void isSimilarEnough_nameLongerThanFourAndSimilarityAtLeastEighty_returnsTrue() {
        // Given
        String leftName = placeNameMatcher.normalize("롯데월드타워");
        String rightName = placeNameMatcher.normalize("롯데월드타워점");

        // When
        boolean isSimilarEnough = placeNameMatcher.isSimilarEnough(leftName, rightName);

        // Then
        assertThat(isSimilarEnough).isTrue();
        assertThat(placeNameMatcher.calculateSimilarityPercent(leftName, rightName)).isGreaterThanOrEqualTo(80);
    }

    @Test
    void isSimilarEnough_nameNotLongerThanFourAndNotExactlyEqual_returnsFalse() {
        // Given
        String leftName = placeNameMatcher.normalize("서울숲");
        String rightName = placeNameMatcher.normalize("서울숩");

        // When
        boolean isSimilarEnough = placeNameMatcher.isSimilarEnough(leftName, rightName);

        // Then
        assertThat(isSimilarEnough).isFalse();
    }

    @Test
    void isSimilarEnough_shortNameEqualAfterNormalization_returnsTrue() {
        // Given
        String leftName = placeNameMatcher.normalize("서울,숲");
        String rightName = placeNameMatcher.normalize("서울 숲");

        // When
        boolean isSimilarEnough = placeNameMatcher.isSimilarEnough(leftName, rightName);

        // Then
        assertThat(isSimilarEnough).isTrue();
    }
}
