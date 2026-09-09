package com.tourswitch.domain.data.service;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccessibilityClassifier {

    private static final List<String> NEGATIVE_EXPRESSIONS = List.of(
            "불가", "불가능", "없음", "미설치", "어려움", "지원하지 않음"
    );
    private static final List<String> POSITIVE_EXPRESSIONS = List.of(
            "가능", "있음", "설치", "구비", "대여", "접근", "이용"
    );

    public boolean isAccessible(String description) {
        if (!StringUtils.hasText(description)) {
            return false;
        }
        String normalized = description.replaceAll("\\s+", "");
        if (NEGATIVE_EXPRESSIONS.stream().anyMatch(normalized::contains)) {
            return false;
        }
        return POSITIVE_EXPRESSIONS.stream().anyMatch(normalized::contains);
    }
}
