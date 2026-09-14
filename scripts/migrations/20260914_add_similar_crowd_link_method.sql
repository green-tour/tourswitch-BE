ALTER TABLE spot_crowd_link
    DROP CHECK spot_crowd_link_chk_1,
    ADD CONSTRAINT chk_spot_crowd_link_match_method
        CHECK (match_method IN ('EXACT', 'NORMALIZED', 'SIMILAR', 'MANUAL'));
