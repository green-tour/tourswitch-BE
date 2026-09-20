-- 여행방 생성 화면의 목표 관광지 수(1~3곳)와 DB 제약을 일치시킨다.
-- 기존 제약은 이름을 유지해, 기준 스키마와 운영 DB가 같은 규칙을 갖도록 한다.
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'travel_room_chk_1') > 0,
    'ALTER TABLE travel_room DROP CHECK travel_room_chk_1',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE travel_room
    ADD CONSTRAINT travel_room_chk_1
        CHECK (course_spot_count BETWEEN 1 AND 3);
