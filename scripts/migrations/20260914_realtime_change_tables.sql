CREATE TABLE IF NOT EXISTS administrative_dong (
    id BIGINT NOT NULL AUTO_INCREMENT,
    region_id BIGINT NOT NULL,
    dong_code VARCHAR(20) NOT NULL,
    dong_name VARCHAR(50) NOT NULL,
    center_latitude DECIMAL(10, 7) NOT NULL,
    center_longitude DECIMAL(10, 7) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_administrative_dong_code (dong_code),
    UNIQUE KEY uk_administrative_dong_region_name (region_id, dong_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_replacement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    course_spot_id BIGINT NOT NULL,
    administrative_dong_id BIGINT NOT NULL,
    previous_content_id VARCHAR(50) NOT NULL,
    replacement_content_id VARCHAR(50) NOT NULL,
    replaced_by_member_id BIGINT NOT NULL,
    radius_meters INT NOT NULL,
    replaced_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_replacement_course (course_id),
    UNIQUE KEY uk_course_replacement_course_spot (course_spot_id),
    CHECK (previous_content_id <> replacement_content_id),
    CHECK (radius_meters > 0)
) ENGINE=InnoDB;
