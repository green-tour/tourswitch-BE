ALTER TABLE member
    ADD COLUMN profile_image_url varchar(500) NULL
    AFTER nickname;
