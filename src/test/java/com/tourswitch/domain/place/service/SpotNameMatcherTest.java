package com.tourswitch.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 집중률은 관광지명으로만 붙일 수 있어, 표기 차이로 매칭이 끊기지 않는지 고정해 둔다.
 */
class SpotNameMatcherTest {

    private static final String 명동 = "명동";

    @ParameterizedTest
    @CsvSource({
            "명 동, 명동",
            "'명동 ', 명동",
            "' 명동', 명동",
            "'서울   숲', 서울숲",
            "DDP, ddp",
    })
    void 띄어쓰기와_영문_대소문자_차이는_같은_키로_묶인다(String left, String right) {
        assertThat(SpotNameMatcher.key(left)).isEqualTo(SpotNameMatcher.key(right));
    }

    /**
     * 자바 정규식의 \s가 못 잡는 공백들. 서로 다른 시스템을 거친 한글 데이터에 섞여 들어온다.
     */
    @ParameterizedTest
    @ValueSource(chars = {
            ' ', // 일반 공백
            '	', // 탭
            ' ', // NBSP
            '　', // 전각 공백
            '​', // 폭 없는 공백
    })
    void ASCII가_아닌_공백도_제거된다(char separator) {
        String withSeparator = "명" + separator + "동";

        assertThat(SpotNameMatcher.key(withSeparator)).isEqualTo(SpotNameMatcher.key(명동));
    }

    @Test
    void 자모가_분리된_표기도_같은_키로_묶인다() {
        String nfc = Normalizer.normalize(명동, Normalizer.Form.NFC);
        String nfd = Normalizer.normalize(명동, Normalizer.Form.NFD);
        assertThat(nfd).isNotEqualTo(nfc);

        assertThat(SpotNameMatcher.key(nfd)).isEqualTo(SpotNameMatcher.key(nfc));
    }

    @Test
    void 전각_영문은_반각과_같은_키로_묶인다() {
        assertThat(SpotNameMatcher.key("ＤＤＰ")).isEqualTo(SpotNameMatcher.key("DDP"));
    }

    @ParameterizedTest
    @CsvSource({
            "명동, 명동성당",
            "북촌·한옥마을, 북촌한옥마을",
            "경복궁, 덕수궁",
    })
    void 서로_다른_장소는_묶이지_않는다(String left, String right) {
        assertThat(SpotNameMatcher.key(left)).isNotEqualTo(SpotNameMatcher.key(right));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "　"})
    void 이름이_비어_있으면_빈_키를_돌려준다(String blank) {
        assertThat(SpotNameMatcher.key(blank)).isEmpty();
    }

    @Test
    void 이름이_없으면_빈_키를_돌려준다() {
        assertThat(SpotNameMatcher.key(null)).isEmpty();
    }
}
