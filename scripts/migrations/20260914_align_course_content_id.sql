-- dev 엔티티가 사용하는 TourAPI contentId 기반 스키마로 빈 코스 관련 테이블을 정렬한다.
-- 기존 행이 있는 환경은 20260913_tourapi_content_id.sql을 먼저 검토해 실행한다.

ALTER TABLE travel_room
    MODIFY COLUMN course_spot_count INT UNSIGNED NOT NULL;

ALTER TABLE room_candidate
    DROP FOREIGN KEY fk_room_candidate_tourist_spot,
    DROP INDEX uk_room_candidate_spot,
    DROP INDEX idx_room_candidate_spot,
    DROP COLUMN tourist_spot_id,
    ADD COLUMN content_id VARCHAR(50) NOT NULL AFTER travel_room_id,
    ADD COLUMN title_snapshot VARCHAR(200) NOT NULL AFTER concentration_grade_snapshot,
    ADD COLUMN image_url_snapshot VARCHAR(500) NULL AFTER title_snapshot,
    ADD COLUMN latitude_snapshot DOUBLE NOT NULL AFTER image_url_snapshot,
    ADD COLUMN longitude_snapshot DOUBLE NOT NULL AFTER latitude_snapshot,
    ADD CONSTRAINT uk_room_candidate UNIQUE (travel_room_id, content_id);

ALTER TABLE course_spot
    DROP FOREIGN KEY fk_course_spot_tourist_spot,
    DROP FOREIGN KEY fk_course_spot_replaced_from,
    DROP INDEX idx_course_spot_tourist_spot,
    DROP COLUMN tourist_spot_id,
    MODIFY COLUMN replaced_from_spot_id VARCHAR(50) NULL,
    ADD COLUMN content_id VARCHAR(50) NOT NULL AFTER course_id;

ALTER TABLE course_extra_candidate
    DROP FOREIGN KEY fk_course_extra_candidate_tourist_spot,
    DROP INDEX uk_course_extra_candidate,
    DROP INDEX idx_course_extra_candidate_spot,
    DROP COLUMN tourist_spot_id,
    ADD COLUMN content_id VARCHAR(50) NOT NULL AFTER anchor_course_spot_id,
    ADD CONSTRAINT uk_course_extra_candidate UNIQUE (course_id, anchor_course_spot_id, content_id);

ALTER TABLE spot_daily_demand
    DROP FOREIGN KEY fk_spot_daily_demand_tourist_spot,
    DROP INDEX uk_spot_daily_demand,
    DROP COLUMN tourist_spot_id,
    ADD COLUMN content_id VARCHAR(50) NOT NULL AFTER id,
    ADD CONSTRAINT uk_spot_daily_demand UNIQUE (content_id, target_date);
