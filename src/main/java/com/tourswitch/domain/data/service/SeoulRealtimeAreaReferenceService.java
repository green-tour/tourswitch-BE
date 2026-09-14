package com.tourswitch.domain.data.service;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeoulRealtimeAreaReferenceService {

    private static final int EXPECTED_AREA_COUNT = 121;
    private static final String AREA_SCRIPT = "classpath:sql/20260913_seoul_realtime_areas.sql";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public int synchronize() {
        if (countAreas() != EXPECTED_AREA_COUNT || countInvalidAreas() > 0) {
            Resource script = resourceLoader.getResource(AREA_SCRIPT);
            new ResourceDatabasePopulator(script).execute(dataSource);
        }
        int areaCount = countAreas();
        if (areaCount != EXPECTED_AREA_COUNT) {
            throw new IllegalStateException("서울 실시간 영역 기준정보는 121개여야 합니다. 현재=" + areaCount);
        }
        return areaCount;
    }

    private int countAreas() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seoul_realtime_area", Integer.class);
        return count == null ? 0 : count;
    }

    private int countInvalidAreas() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM seoul_realtime_area WHERE ST_IsValid(boundary) = FALSE
                """, Integer.class);
        return count == null ? 0 : count;
    }
}
