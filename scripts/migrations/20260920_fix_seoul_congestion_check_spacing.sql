-- 20260915_base_schema.sql이 공백 없는 '약간붐빔'으로 CHECK 제약을 되돌려 놓아
-- 서울시 API가 '약간 붐빔'을 반환하는 시점부터 혼잡도 적재가 전부 실패한다.
-- 20260913_fix_seoul_congestion_check.sql과 같은 값으로 다시 교체한다.
--
-- 한글 리터럴이 접속 세션 collation으로 해석되면 CHECK 비교가 거부되므로
-- 스크립트가 직접 세션 문자셋을 고정한다(트러블슈팅 5절).
SET NAMES utf8mb4;

-- 제약 이름이 환경에 따라 다르다. base_schema로 생성한 DB는 seoul_realtime_population_chk_1,
-- 20260913 마이그레이션을 적용한 DB는 chk_seoul_realtime_population_congestion_level이다.
-- 존재하는 쪽만 제거하고 재실행해도 안전하도록 한다.
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND CONSTRAINT_NAME = 'seoul_realtime_population_chk_1') > 0,
    'ALTER TABLE seoul_realtime_population DROP CHECK seoul_realtime_population_chk_1',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND CONSTRAINT_NAME = 'chk_seoul_realtime_population_congestion_level') > 0,
    'ALTER TABLE seoul_realtime_population DROP CHECK chk_seoul_realtime_population_congestion_level',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE seoul_realtime_population
    ADD CONSTRAINT chk_seoul_realtime_population_congestion_level
        CHECK (congestion_level IS NULL OR congestion_level IN ('여유', '보통', '약간 붐빔', '붐빔'));

SELECT CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_seoul_realtime_population_congestion_level';
