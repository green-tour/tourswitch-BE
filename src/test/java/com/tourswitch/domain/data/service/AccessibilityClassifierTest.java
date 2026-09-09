package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessibilityClassifierTest {

    private final AccessibilityClassifier classifier = new AccessibilityClassifier();

    @Test
    void isAccessible_positiveDescription_returnsTrue() {
        assertThat(classifier.isAccessible("주출입구에 경사로가 있어 휠체어 접근 가능함")).isTrue();
    }

    @Test
    void isAccessible_negativeExpressionHasPriority_returnsFalse() {
        assertThat(classifier.isAccessible("휠체어 접근이 불가능하며 대여 시설 없음")).isFalse();
    }

    @Test
    void isAccessible_blankDescription_returnsFalse() {
        assertThat(classifier.isAccessible(" ")).isFalse();
    }
}
