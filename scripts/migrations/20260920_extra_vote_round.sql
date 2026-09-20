-- 추가 투표 라운드 도입.
-- 관광지 투표가 끝나면 확정 경유지 기준으로 부가 후보를 만들고, 방 상태를 EXTRA_VOTING으로 두어
-- 참여자가 음식점·숙박·쇼핑을 한 번 더 고르게 한다. 그 결과를 합쳐 코스를 확정한다.
--
-- 한글 리터럴이 접속 세션 collation으로 해석되면 CHECK 비교가 거부되므로 세션 문자셋을 고정한다(트러블슈팅 5절).
SET NAMES utf8mb4;

-- 1. 방 상태에 EXTRA_VOTING 추가
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'travel_room_chk_2') > 0,
    'ALTER TABLE travel_room DROP CHECK travel_room_chk_2',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE travel_room
    ADD CONSTRAINT travel_room_chk_2
        CHECK (status IN ('VOTING', 'EXTRA_VOTING', 'CLOSED', 'COURSE_CONFIRMED'));

-- 2. 부가 후보 카드에 노출할 이름·이미지 스냅샷.
--    설명은 목록 응답에 없어 사용자가 후보를 고를 때 상세 조회로 채운다.
ALTER TABLE course_extra_candidate
    ADD COLUMN title_snapshot VARCHAR(200) NULL AFTER content_id,
    ADD COLUMN image_url_snapshot VARCHAR(500) NULL AFTER title_snapshot;

-- 3. 추가 투표 기록. 참여자 한 명이 후보 하나에 한 표만 던진다.
CREATE TABLE IF NOT EXISTS course_extra_vote (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_extra_candidate_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_extra_vote (course_extra_candidate_id, member_id),
    KEY idx_course_extra_vote_member (member_id),
    CONSTRAINT fk_course_extra_vote_candidate FOREIGN KEY (course_extra_candidate_id)
        REFERENCES course_extra_candidate (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_course_extra_vote_member FOREIGN KEY (member_id)
        REFERENCES member (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='추가 투표 라운드의 참여자별 선택';

-- 4. 라운드가 둘이므로 완료 플래그를 라운드별로 구분한다.
ALTER TABLE room_participant
    ADD COLUMN is_extra_selection_completed TINYINT(1) NOT NULL DEFAULT 0 AFTER is_selection_completed;

SELECT
    (SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'travel_room_chk_2') AS room_status_check,
    (SELECT COUNT(*) FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_extra_vote') AS extra_vote_table;
