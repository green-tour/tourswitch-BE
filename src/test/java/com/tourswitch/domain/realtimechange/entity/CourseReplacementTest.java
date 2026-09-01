package com.tourswitch.domain.realtimechange.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CourseReplacementTest {

    @Test
    void 교체_이력을_생성하면_검색_반경은_3km로_고정된다() {
        CourseReplacement replacement = CourseReplacement.create(1L, 2L, 3L, 10L, 20L, 30L);

        assertThat(replacement.getRadiusMeters()).isEqualTo(3_000);
        assertThat(replacement.getPreviousTouristSpotId()).isEqualTo(10L);
        assertThat(replacement.getReplacementTouristSpotId()).isEqualTo(20L);
    }

    @Test
    void 기존_장소와_대체_장소가_같으면_생성할_수_없다() {
        assertThatThrownBy(() -> CourseReplacement.create(1L, 2L, 3L, 10L, 10L, 30L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
