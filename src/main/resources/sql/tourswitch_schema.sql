-- 투어스위치 물리 DDL
-- 대상: MySQL 8.4+
-- 설계 원문: 투어스위치_DB설계.md

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS tourswitch
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE tourswitch;

CREATE TABLE region (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    area_code VARCHAR(20) NOT NULL,
    area_name VARCHAR(100) NOT NULL,
    district_code VARCHAR(20) NOT NULL,
    district_name VARCHAR(100) NOT NULL,
    legal_dong_area_code VARCHAR(20) NULL,
    legal_dong_district_code VARCHAR(20) NULL,
    center_latitude DECIMAL(10,7) NULL,
    center_longitude DECIMAL(10,7) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_area_district (area_code, district_code)
) ENGINE=InnoDB;

CREATE TABLE keyword (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    keyword_name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_keyword_name (keyword_name),
    CHECK (display_order > 0)
) ENGINE=InnoDB;

CREATE TABLE keyword_classification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    keyword_id BIGINT UNSIGNED NOT NULL,
    classification_level2_code VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_keyword_classification (keyword_id, classification_level2_code),
    CONSTRAINT fk_keyword_classification_keyword
        FOREIGN KEY (keyword_id) REFERENCES keyword (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE tourist_spot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    content_id VARCHAR(50) NOT NULL,
    content_type_id INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500) NOT NULL,
    address VARCHAR(1000) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    location_point POINT SRID 4326 NOT NULL,
    first_image_url VARCHAR(2000) NULL,
    overview TEXT NULL,
    classification_level1_code VARCHAR(20) NULL,
    classification_level2_code VARCHAR(20) NULL,
    classification_level3_code VARCHAR(20) NULL,
    region_id BIGINT UNSIGNED NULL,
    event_start_date DATE NULL,
    event_end_date DATE NULL,
    is_coordinate_valid BOOLEAN NOT NULL DEFAULT TRUE,
    has_crowd_data BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    data_synced_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tourist_spot_content_id (content_id),
    KEY idx_tourist_spot_region (region_id),
    KEY idx_tourist_spot_candidate (region_id, content_type_id, is_active),
    KEY idx_tourist_spot_normalized_title (normalized_title),
    SPATIAL INDEX sx_tourist_spot_location (location_point),
    CONSTRAINT fk_tourist_spot_region
        FOREIGN KEY (region_id) REFERENCES region (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (content_type_id IN (12, 14, 15, 28, 32, 38, 39)),
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CHECK (event_end_date IS NULL OR event_start_date IS NULL OR event_end_date >= event_start_date)
) ENGINE=InnoDB;

CREATE TABLE spot_accessibility (
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    has_wheelchair_access BOOLEAN NOT NULL DEFAULT FALSE,
    has_stroller_access BOOLEAN NOT NULL DEFAULT FALSE,
    wheelchair_description TEXT NULL,
    stroller_description TEXT NULL,
    barrier_free_detail JSON NULL,
    synced_at DATETIME NOT NULL,
    PRIMARY KEY (tourist_spot_id),
    CONSTRAINT fk_spot_accessibility_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE spot_keyword_link (
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    keyword_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (tourist_spot_id, keyword_id),
    CONSTRAINT fk_spot_keyword_link_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_spot_keyword_link_keyword
        FOREIGN KEY (keyword_id) REFERENCES keyword (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE spot_crowd_forecast (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    area_code VARCHAR(20) NOT NULL,
    district_code VARCHAR(20) NOT NULL,
    district_name VARCHAR(100) NOT NULL,
    attraction_name VARCHAR(500) NOT NULL,
    normalized_attraction_name VARCHAR(500) NOT NULL,
    forecast_date DATE NOT NULL,
    concentration_rate DECIMAL(6,3) NOT NULL,
    concentration_percentile DECIMAL(7,6) NULL,
    concentration_grade VARCHAR(20) NULL,
    collected_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_spot_crowd_forecast_natural (district_code, attraction_name, forecast_date),
    KEY idx_spot_crowd_forecast_lookup (district_code, normalized_attraction_name, forecast_date),
    CHECK (concentration_rate BETWEEN 0 AND 100),
    CHECK (concentration_percentile IS NULL OR concentration_percentile BETWEEN 0 AND 1),
    CHECK (concentration_grade IS NULL OR concentration_grade IN ('여유', '보통', '붐빔', '매우 붐빔'))
) ENGINE=InnoDB;

CREATE TABLE crowd_grade_threshold (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    forecast_date DATE NOT NULL,
    percentile_25_value DECIMAL(6,3) NOT NULL,
    percentile_50_value DECIMAL(6,3) NOT NULL,
    percentile_75_value DECIMAL(6,3) NOT NULL,
    sample_count INT UNSIGNED NOT NULL,
    calculated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_crowd_grade_threshold_date (forecast_date),
    CHECK (percentile_25_value <= percentile_50_value),
    CHECK (percentile_50_value <= percentile_75_value),
    CHECK (sample_count > 0)
) ENGINE=InnoDB;

CREATE TABLE seoul_realtime_area (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    area_code VARCHAR(20) NULL,
    area_name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    boundary POLYGON SRID 4326 NOT NULL,
    reference_population_max INT UNSIGNED NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seoul_realtime_area_name (area_name),
    UNIQUE KEY uk_seoul_realtime_area_code (area_code),
    SPATIAL INDEX sx_seoul_realtime_area_boundary (boundary),
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB;

CREATE TABLE seoul_realtime_population (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seoul_realtime_area_id BIGINT UNSIGNED NOT NULL,
    congestion_level VARCHAR(30) NULL,
    congestion_message VARCHAR(500) NULL,
    population_min INT UNSIGNED NULL,
    population_max INT UNSIGNED NULL,
    resident_rate DECIMAL(7,4) NULL,
    non_resident_rate DECIMAL(7,4) NULL,
    observed_at DATETIME NOT NULL,
    collected_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_realtime_population_area_observed (seoul_realtime_area_id, observed_at),
    CONSTRAINT fk_realtime_population_area
        FOREIGN KEY (seoul_realtime_area_id) REFERENCES seoul_realtime_area (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (population_max IS NULL OR population_min IS NULL OR population_max >= population_min),
    CHECK (resident_rate IS NULL OR resident_rate BETWEEN 0 AND 100),
    CHECK (non_resident_rate IS NULL OR non_resident_rate BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE seoul_realtime_forecast (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seoul_realtime_area_id BIGINT UNSIGNED NOT NULL,
    forecast_time DATETIME NOT NULL,
    congestion_level VARCHAR(30) NULL,
    population_min INT UNSIGNED NULL,
    population_max INT UNSIGNED NULL,
    collected_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_realtime_forecast_snapshot (seoul_realtime_area_id, forecast_time, collected_at),
    KEY idx_realtime_forecast_time (forecast_time),
    CONSTRAINT fk_realtime_forecast_area
        FOREIGN KEY (seoul_realtime_area_id) REFERENCES seoul_realtime_area (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (population_max IS NULL OR population_min IS NULL OR population_max >= population_min)
) ENGINE=InnoDB;

CREATE TABLE spot_crowd_link (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    attraction_name VARCHAR(500) NOT NULL,
    district_code VARCHAR(20) NOT NULL,
    match_method VARCHAR(20) NOT NULL,
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    matched_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_spot_crowd_link_natural (attraction_name, district_code),
    CONSTRAINT fk_spot_crowd_link_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (match_method IN ('EXACT', 'NORMALIZED', 'MANUAL'))
) ENGINE=InnoDB;

CREATE TABLE spot_area_link (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    seoul_realtime_area_id BIGINT UNSIGNED NOT NULL,
    match_method VARCHAR(30) NOT NULL,
    distance_meters INT UNSIGNED NOT NULL,
    name_match_priority TINYINT UNSIGNED NOT NULL,
    area_size_square_meters DECIMAL(14,3) NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    primary_marker BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN is_primary THEN tourist_spot_id ELSE NULL END
    ) STORED,
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    matched_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_spot_area_link_pair (tourist_spot_id, seoul_realtime_area_id),
    UNIQUE KEY uk_spot_area_link_primary (primary_marker),
    KEY idx_spot_area_link_area (seoul_realtime_area_id),
    CONSTRAINT fk_spot_area_link_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_spot_area_link_area
        FOREIGN KEY (seoul_realtime_area_id) REFERENCES seoul_realtime_area (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (match_method IN ('INSIDE_BOUNDARY', 'PROXIMITY', 'MANUAL')),
    CHECK (name_match_priority BETWEEN 1 AND 4)
) ENGINE=InnoDB;

CREATE TABLE spot_duplicate_link (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    canonical_tourist_spot_id BIGINT UNSIGNED NOT NULL,
    match_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    matched_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_spot_duplicate_link_duplicate (tourist_spot_id),
    CONSTRAINT fk_spot_duplicate_link_duplicate
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_spot_duplicate_link_canonical
        FOREIGN KEY (canonical_tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (tourist_spot_id <> canonical_tourist_spot_id),
    CHECK (match_method = 'MANUAL')
) ENGINE=InnoDB;

CREATE TABLE member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    social_provider VARCHAR(20) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    refresh_token_hash CHAR(64) NULL,
    refresh_token_expires_at DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    withdrawn_at DATETIME NULL,
    purged_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_social (social_provider, social_id),
    CHECK (social_provider IN ('KAKAO', 'NAVER')),
    CHECK (status IN ('ACTIVE', 'WITHDRAWN', 'PURGED'))
) ENGINE=InnoDB;

CREATE TABLE travel_room (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    invite_token CHAR(64) NOT NULL,
    host_member_id BIGINT UNSIGNED NOT NULL,
    room_name VARCHAR(200) NULL,
    travel_date DATE NOT NULL,
    region_id BIGINT UNSIGNED NOT NULL,
    course_spot_count TINYINT UNSIGNED NOT NULL,
    includes_food BOOLEAN NOT NULL DEFAULT FALSE,
    includes_lodging BOOLEAN NOT NULL DEFAULT FALSE,
    includes_shopping BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'VOTING',
    recommendation_condition_key CHAR(64) NOT NULL,
    candidate_offset INT UNSIGNED NOT NULL DEFAULT 0,
    closed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_travel_room_invite_token (invite_token),
    KEY idx_travel_room_host (host_member_id),
    KEY idx_travel_room_region_date (region_id, travel_date),
    CONSTRAINT fk_travel_room_host
        FOREIGN KEY (host_member_id) REFERENCES member (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_travel_room_region
        FOREIGN KEY (region_id) REFERENCES region (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (course_spot_count BETWEEN 3 AND 6),
    CHECK (status IN ('VOTING', 'CLOSED', 'COURSE_CONFIRMED'))
) ENGINE=InnoDB;

CREATE TABLE room_keyword (
    travel_room_id BIGINT UNSIGNED NOT NULL,
    keyword_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (travel_room_id, keyword_id),
    CONSTRAINT fk_room_keyword_room
        FOREIGN KEY (travel_room_id) REFERENCES travel_room (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_room_keyword_keyword
        FOREIGN KEY (keyword_id) REFERENCES keyword (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE room_participant (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    travel_room_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    is_host BOOLEAN NOT NULL DEFAULT FALSE,
    is_selection_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_participant_member (travel_room_id, member_id),
    KEY idx_room_participant_member (member_id),
    CONSTRAINT fk_room_participant_room
        FOREIGN KEY (travel_room_id) REFERENCES travel_room (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_room_participant_member
        FOREIGN KEY (member_id) REFERENCES member (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE room_candidate (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    travel_room_id BIGINT UNSIGNED NOT NULL,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    keyword_id BIGINT UNSIGNED NULL,
    display_order INT UNSIGNED NOT NULL,
    recommendation_score DECIMAL(12,8) NULL,
    concentration_rate_snapshot DECIMAL(6,3) NULL,
    concentration_grade_snapshot VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_candidate_spot (travel_room_id, tourist_spot_id),
    UNIQUE KEY uk_room_candidate_order (travel_room_id, display_order),
    KEY idx_room_candidate_spot (tourist_spot_id),
    CONSTRAINT fk_room_candidate_room
        FOREIGN KEY (travel_room_id) REFERENCES travel_room (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_room_candidate_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_room_candidate_keyword
        FOREIGN KEY (keyword_id) REFERENCES keyword (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (concentration_rate_snapshot IS NULL OR concentration_rate_snapshot BETWEEN 0 AND 100),
    CHECK (concentration_grade_snapshot IS NULL OR concentration_grade_snapshot IN ('여유', '보통', '붐빔', '매우 붐빔'))
) ENGINE=InnoDB;

CREATE TABLE room_vote (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    room_candidate_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    voted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_vote_member (room_candidate_id, member_id),
    KEY idx_room_vote_member (member_id),
    CONSTRAINT fk_room_vote_candidate
        FOREIGN KEY (room_candidate_id) REFERENCES room_candidate (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_room_vote_member
        FOREIGN KEY (member_id) REFERENCES member (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE recommendation_condition_counter (
    recommendation_condition_key CHAR(64) NOT NULL,
    room_count INT UNSIGNED NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_condition_key),
    CHECK (room_count >= 0)
) ENGINE=InnoDB;

CREATE TABLE course (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    travel_room_id BIGINT UNSIGNED NOT NULL,
    travel_date DATE NOT NULL,
    total_distance_meters INT UNSIGNED NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    confirmed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_room (travel_room_id),
    CONSTRAINT fk_course_room
        FOREIGN KEY (travel_room_id) REFERENCES travel_room (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (status IN ('DRAFT', 'CONFIRMED'))
) ENGINE=InnoDB;

CREATE TABLE course_spot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    spot_role VARCHAR(20) NOT NULL,
    visit_order INT UNSIGNED NOT NULL,
    spot_title_snapshot VARCHAR(500) NOT NULL,
    concentration_rate_snapshot DECIMAL(6,3) NULL,
    vote_count_snapshot INT UNSIGNED NULL,
    is_replaced BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_from_spot_id BIGINT UNSIGNED NULL,
    replaced_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_spot_order (course_id, visit_order),
    KEY idx_course_spot_tourist_spot (tourist_spot_id),
    CONSTRAINT fk_course_spot_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_course_spot_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_course_spot_replaced_from
        FOREIGN KEY (replaced_from_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (spot_role IN ('ATTRACTION', 'FOOD', 'LODGING', 'SHOPPING')),
    CHECK (concentration_rate_snapshot IS NULL OR concentration_rate_snapshot BETWEEN 0 AND 100),
    CHECK (is_replaced = FALSE OR replaced_from_spot_id IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE course_extra_candidate (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    anchor_course_spot_id BIGINT UNSIGNED NOT NULL,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    spot_role VARCHAR(20) NOT NULL,
    distance_meters INT UNSIGNED NOT NULL,
    display_order INT UNSIGNED NOT NULL,
    is_selected BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_extra_candidate (course_id, anchor_course_spot_id, tourist_spot_id),
    KEY idx_course_extra_candidate_spot (tourist_spot_id),
    CONSTRAINT fk_course_extra_candidate_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_course_extra_candidate_anchor
        FOREIGN KEY (anchor_course_spot_id) REFERENCES course_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_course_extra_candidate_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CHECK (spot_role IN ('FOOD', 'LODGING', 'SHOPPING'))
) ENGINE=InnoDB;

CREATE TABLE spot_daily_demand (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tourist_spot_id BIGINT UNSIGNED NOT NULL,
    target_date DATE NOT NULL,
    participant_count INT UNSIGNED NOT NULL DEFAULT 0,
    course_count INT UNSIGNED NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_spot_daily_demand (tourist_spot_id, target_date),
    CONSTRAINT fk_spot_daily_demand_tourist_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;


