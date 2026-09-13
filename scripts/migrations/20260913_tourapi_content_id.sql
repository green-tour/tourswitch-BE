-- TourAPI 실시간 조회 전환용 1회성 MySQL 8 마이그레이션.
-- 실행 대상: tourist_spot_id(BIGINT)를 사용하는 기존 스키마.
-- DDL은 MySQL에서 자동 커밋되므로 실행 전 반드시 DB 백업을 만든다.

-- 1. 기존 FK를 유지한 채 content_id를 복사한다. 매핑 불가 행이 있으면 아래 NOT NULL 변경에서
--    실패하므로, 기존 ID 컬럼을 삭제하기 전에 데이터 이상을 발견할 수 있다.
ALTER TABLE room_candidate ADD COLUMN content_id_new VARCHAR(50) NULL AFTER travel_room_id;
UPDATE room_candidate rc
JOIN tourist_spot ts ON ts.id = rc.tourist_spot_id
SET rc.content_id_new = ts.content_id;
ALTER TABLE room_candidate MODIFY content_id_new VARCHAR(50) NOT NULL;

ALTER TABLE course_spot
    ADD COLUMN content_id_new VARCHAR(50) NULL AFTER course_id,
    ADD COLUMN replaced_from_content_id_new VARCHAR(50) NULL AFTER is_replaced;
UPDATE course_spot cs
JOIN tourist_spot ts ON ts.id = cs.tourist_spot_id
SET cs.content_id_new = ts.content_id;
UPDATE course_spot cs
JOIN tourist_spot ts ON ts.id = cs.replaced_from_spot_id
SET cs.replaced_from_content_id_new = ts.content_id;
ALTER TABLE course_spot MODIFY content_id_new VARCHAR(50) NOT NULL;

ALTER TABLE course_extra_candidate ADD COLUMN content_id_new VARCHAR(50) NULL AFTER anchor_course_spot_id;
UPDATE course_extra_candidate cec
JOIN tourist_spot ts ON ts.id = cec.tourist_spot_id
SET cec.content_id_new = ts.content_id;
ALTER TABLE course_extra_candidate MODIFY content_id_new VARCHAR(50) NOT NULL;

ALTER TABLE spot_daily_demand ADD COLUMN content_id_new VARCHAR(50) NULL AFTER id;
UPDATE spot_daily_demand sdd
JOIN tourist_spot ts ON ts.id = sdd.tourist_spot_id
SET sdd.content_id_new = ts.content_id;
ALTER TABLE spot_daily_demand MODIFY content_id_new VARCHAR(50) NOT NULL;

ALTER TABLE course_replacement
    ADD COLUMN previous_content_id_new VARCHAR(50) NULL AFTER administrative_dong_id,
    ADD COLUMN replacement_content_id_new VARCHAR(50) NULL AFTER previous_content_id_new;
UPDATE course_replacement cr
JOIN tourist_spot previous_spot ON previous_spot.id = cr.previous_tourist_spot_id
JOIN tourist_spot replacement_spot ON replacement_spot.id = cr.replacement_tourist_spot_id
SET cr.previous_content_id_new = previous_spot.content_id,
    cr.replacement_content_id_new = replacement_spot.content_id;
ALTER TABLE course_replacement
    MODIFY previous_content_id_new VARCHAR(50) NOT NULL,
    MODIFY replacement_content_id_new VARCHAR(50) NOT NULL;

-- 2. 관광지 마스터 FK와 기존 ID 컬럼을 제거하고 content_id를 정식 컬럼으로 바꾼다.
ALTER TABLE room_candidate
    DROP FOREIGN KEY fk_room_candidate_tourist_spot,
    DROP INDEX uk_room_candidate,
    DROP INDEX idx_room_candidate_spot,
    DROP COLUMN tourist_spot_id,
    RENAME COLUMN content_id_new TO content_id,
    ADD CONSTRAINT uk_room_candidate UNIQUE (travel_room_id, content_id);

ALTER TABLE room_candidate
    ADD COLUMN title_snapshot VARCHAR(200) NULL AFTER concentration_grade_snapshot,
    ADD COLUMN image_url_snapshot VARCHAR(500) NULL AFTER title_snapshot,
    ADD COLUMN latitude_snapshot DOUBLE NULL AFTER image_url_snapshot,
    ADD COLUMN longitude_snapshot DOUBLE NULL AFTER latitude_snapshot;
UPDATE room_candidate rc
JOIN tourist_spot ts ON ts.content_id = rc.content_id
SET rc.title_snapshot = LEFT(ts.title, 200),
    rc.image_url_snapshot = LEFT(ts.first_image_url, 500),
    rc.latitude_snapshot = ts.latitude,
    rc.longitude_snapshot = ts.longitude;
ALTER TABLE room_candidate
    MODIFY title_snapshot VARCHAR(200) NOT NULL,
    MODIFY latitude_snapshot DOUBLE NOT NULL,
    MODIFY longitude_snapshot DOUBLE NOT NULL;

ALTER TABLE course_spot
    DROP FOREIGN KEY fk_course_spot_tourist_spot,
    DROP FOREIGN KEY fk_course_spot_replaced_from,
    DROP INDEX idx_course_spot_tourist_spot,
    DROP INDEX idx_course_spot_replaced_from,
    DROP COLUMN tourist_spot_id,
    DROP COLUMN replaced_from_spot_id,
    RENAME COLUMN content_id_new TO content_id,
    RENAME COLUMN replaced_from_content_id_new TO replaced_from_spot_id;

ALTER TABLE course_extra_candidate
    DROP FOREIGN KEY fk_course_extra_candidate_tourist_spot,
    DROP INDEX uk_course_extra_candidate,
    DROP INDEX idx_course_extra_candidate_spot,
    DROP COLUMN tourist_spot_id,
    RENAME COLUMN content_id_new TO content_id,
    ADD CONSTRAINT uk_course_extra_candidate UNIQUE (course_id, anchor_course_spot_id, content_id);

ALTER TABLE spot_daily_demand
    DROP FOREIGN KEY fk_spot_daily_demand_tourist_spot,
    DROP INDEX uk_spot_daily_demand,
    DROP COLUMN tourist_spot_id,
    RENAME COLUMN content_id_new TO content_id,
    ADD CONSTRAINT uk_spot_daily_demand UNIQUE (content_id, target_date);

ALTER TABLE course_replacement
    DROP CHECK chk_course_replacement_different_spot,
    DROP FOREIGN KEY fk_course_replacement_previous_spot,
    DROP FOREIGN KEY fk_course_replacement_new_spot,
    DROP INDEX idx_course_replacement_previous_spot,
    DROP INDEX idx_course_replacement_new_spot,
    DROP COLUMN previous_tourist_spot_id,
    DROP COLUMN replacement_tourist_spot_id,
    RENAME COLUMN previous_content_id_new TO previous_content_id,
    RENAME COLUMN replacement_content_id_new TO replacement_content_id,
    ADD CONSTRAINT chk_course_replacement_different_content
        CHECK (previous_content_id <> replacement_content_id);

-- 3. 결과 확인. 모든 장소 식별자는 이제 TourAPI contentId 문자열이다.
SHOW COLUMNS FROM room_candidate;
SHOW COLUMNS FROM course_spot;
SHOW COLUMNS FROM course_extra_candidate;
SHOW COLUMNS FROM spot_daily_demand;
SHOW COLUMNS FROM course_replacement;
