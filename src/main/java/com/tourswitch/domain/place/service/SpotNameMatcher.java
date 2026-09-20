package com.tourswitch.domain.place.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * TatsCnctrRateService는 contentId를 주지 않아 관광지명으로만 집중률을 붙일 수 있다.
 * 두 API가 같은 장소를 다르게 표기하면 매칭이 조용히 끊기므로, 양쪽 이름을 같은 규칙으로
 * 정규화한 키로 맞춘다.
 *
 * 지우는 것은 공백류뿐이다. 가운뎃점이나 괄호 같은 기호는 남긴다. 그것까지 지우면
 * 서로 다른 장소가 같은 키로 합쳐질 수 있다.
 */
final class SpotNameMatcher {

    private SpotNameMatcher() {
    }

    /**
     * 정규화한 매칭 키. 이름이 없거나 공백뿐이면 빈 문자열이며, 호출부는 빈 키를
     * 매칭에 쓰지 않는다(이름 없는 장소끼리 서로 붙는 것을 막는다).
     */
    static String key(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        // NFKC는 자모 분리(NFD) 표기를 합치고 전각 영숫자를 반각으로 되돌린다.
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC);
        StringBuilder builder = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (!isRemovable(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
        });
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * 자바 정규식의 \s는 ASCII 공백만 잡아 NBSP(U+00A0)나 전각 공백(U+3000),
     * 폭 없는 공백(U+200B)을 놓친다. 그래서 문자 종류로 직접 거른다.
     */
    private static boolean isRemovable(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || type == Character.FORMAT
                || type == Character.CONTROL;
    }
}
