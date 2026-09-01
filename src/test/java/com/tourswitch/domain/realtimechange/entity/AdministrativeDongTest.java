package com.tourswitch.domain.realtimechange.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AdministrativeDongTest {

    @Test
    void 행정동을_생성하면_활성_상태가_된다() {
        AdministrativeDong dong = AdministrativeDong.create(
                1L,
                "1111051500",
                "청운효자동",
                new BigDecimal("37.5841000"),
                new BigDecimal("126.9707000"));

        assertThat(dong.getIsActive()).isTrue();
        assertThat(dong.getDongName()).isEqualTo("청운효자동");
    }

    @Test
    void 행정동을_비활성화할_수_있다() {
        AdministrativeDong dong = AdministrativeDong.create(
                1L,
                "1111051500",
                "청운효자동",
                new BigDecimal("37.5841000"),
                new BigDecimal("126.9707000"));

        dong.deactivate();

        assertThat(dong.getIsActive()).isFalse();
    }
}
