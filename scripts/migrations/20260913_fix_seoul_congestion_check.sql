-- 기존 DDL 실행 시 깨진 한글 혼잡도 CHECK 값을 정상 UTF-8 값으로 교체한다.
ALTER TABLE seoul_realtime_population
    DROP CHECK seoul_realtime_population_chk_1;

ALTER TABLE seoul_realtime_population
    ADD CONSTRAINT chk_seoul_realtime_population_congestion_level
        CHECK (congestion_level IS NULL OR congestion_level IN ('여유', '보통', '약간 붐빔', '붐빔'));
