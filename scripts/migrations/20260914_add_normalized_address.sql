ALTER TABLE tourist_spot
    ADD COLUMN normalized_address VARCHAR(1000) NOT NULL DEFAULT '' AFTER address,
    ADD INDEX idx_tourist_spot_search
        (region_id, classification_level2_code, content_type_id, is_active);

UPDATE tourist_spot
SET normalized_address = LOWER(REGEXP_REPLACE(COALESCE(address, ''), '[^[:alnum:]]', ''));
