package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class SeoulAreaValidityDiagnosticTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 모든_서울_실시간_영역은_유효한_폴리곤이다() {
        List<String> invalidAreas = jdbcTemplate.queryForList("""
                SELECT CONCAT(area_code, ':', area_name)
                FROM seoul_realtime_area
                WHERE ST_IsValid(boundary) = FALSE
                """, String.class);
        assertThat(invalidAreas).as("invalid areas: %s", invalidAreas).isEmpty();
    }
}
