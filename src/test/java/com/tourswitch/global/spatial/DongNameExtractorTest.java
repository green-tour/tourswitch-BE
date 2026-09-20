package com.tourswitch.global.spatial;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 실제 TourAPI 주소 표기를 기준으로 고정한다(2026-09-21 areaBasedList2 응답 확인).
 */
class DongNameExtractorTest {

    @ParameterizedTest
    @CsvSource({
            "'서울특별시 종로구 북촌로 57 (가회동)', 가회동",
            "'서울특별시 종로구 창경궁로 185 (명륜3가)', 명륜3가",
            "'서울특별시 중구 을지로 지하 30 (을지로1가, 지하도상가)', 을지로1가",
            "'서울특별시 종로구 가회동 1-1', 가회동",
            "'서울특별시 강남구 삼성동 159', 삼성동",
    })
    void 도로명과_지번_주소에서_동을_뽑는다(String address, String expected) {
        assertThat(DongNameExtractor.extract(address)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "서울특별시 종로구 세종대로 175",
            "경기도 성남시 분당구 판교로 어딘가",
    })
    void 동을_알_수_없으면_비운다(String address) {
        assertThat(DongNameExtractor.extract(address)).isEmpty();
    }

    @Test
    void 괄호가_여러_개면_동이_있는_쪽을_쓴다() {
        assertThat(DongNameExtractor.extract("서울특별시 마포구 와우산로 94 (상수동) (홍익대학교)"))
                .contains("상수동");
    }
}
