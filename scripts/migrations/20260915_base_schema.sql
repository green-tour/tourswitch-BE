
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nickname` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `withdrawn_at` datetime DEFAULT NULL,
  `purged_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `refresh_token_expires_at` datetime(6) DEFAULT NULL,
  `refresh_token_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `social_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `social_provider` enum('KAKAO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_social_provider_social_id` (`social_provider`,`social_id`),
  CONSTRAINT `member_chk_1` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'WITHDRAWN',_utf8mb4'PURGED')))
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='탈퇴 시 status=WITHDRAWN, 1년 후 배치가 익명화하고 status=PURGED(7.1절). 물리 삭제하지 않음';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `travel_room_id` bigint NOT NULL,
  `travel_date` date NOT NULL,
  `total_distance_meters` int DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `confirmed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_travel_room` (`travel_room_id`),
  CONSTRAINT `fk_course_travel_room` FOREIGN KEY (`travel_room_id`) REFERENCES `travel_room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `course_chk_1` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'CONFIRMED')))
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방 하나에 코스 하나(8.1절). 영구 보존(7.1절 보존정책 확정, 9차 검수)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_extra_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `anchor_course_spot_id` bigint NOT NULL COMMENT '기준이 된 코스 경유지',
  `content_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spot_role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `distance_meters` int NOT NULL,
  `display_order` int NOT NULL,
  `is_selected` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_extra_candidate` (`course_id`,`anchor_course_spot_id`,`content_id`),
  KEY `idx_course_extra_candidate_spot` (`content_id`),
  KEY `fk_course_extra_candidate_anchor` (`anchor_course_spot_id`),
  CONSTRAINT `fk_course_extra_candidate_anchor` FOREIGN KEY (`anchor_course_spot_id`) REFERENCES `course_spot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_course_extra_candidate_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `course_extra_candidate_chk_1` CHECK ((`spot_role` in (_utf8mb4'FOOD',_utf8mb4'LODGING',_utf8mb4'SHOPPING')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방장 단독 선택(8.3절). 영구 보존(7.1절 보존정책 확정, 9차 검수)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_spot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `content_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spot_role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `visit_order` int NOT NULL,
  `spot_title_snapshot` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `concentration_rate_snapshot` decimal(5,2) DEFAULT NULL,
  `vote_count_snapshot` int DEFAULT NULL,
  `is_replaced` tinyint(1) NOT NULL DEFAULT '0',
  `replaced_from_spot_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `replaced_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_spot` (`course_id`,`visit_order`),
  KEY `idx_course_spot_replaced_from` (`replaced_from_spot_id`),
  KEY `idx_course_spot_tourist_spot` (`content_id`),
  CONSTRAINT `fk_course_spot_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `course_spot_chk_1` CHECK ((`spot_role` in (_utf8mb4'ATTRACTION',_utf8mb4'FOOD',_utf8mb4'LODGING',_utf8mb4'SHOPPING')))
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='당일 교체 시 concentration_rate_snapshot/vote_count_snapshot을 NULL로 비움(8.2절). 영구 보존';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_daily_demand` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_date` date NOT NULL,
  `participant_count` int NOT NULL DEFAULT '0',
  `course_count` int NOT NULL DEFAULT '0' COMMENT '참고용',
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spot_daily_demand` (`content_id`,`target_date`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='집계 시점은 코스 CONFIRMED, spot_role=ATTRACTION만 집계(9.4절). 원자적 UPSERT로 갱신';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `administrative_dong` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint NOT NULL,
  `dong_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dong_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `center_latitude` decimal(10,7) NOT NULL,
  `center_longitude` decimal(10,7) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_administrative_dong_code` (`dong_code`),
  UNIQUE KEY `uk_administrative_dong_region_name` (`region_id`,`dong_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_replacement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `course_spot_id` bigint NOT NULL,
  `administrative_dong_id` bigint NOT NULL,
  `previous_content_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `replacement_content_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `replaced_by_member_id` bigint NOT NULL,
  `radius_meters` int NOT NULL,
  `replaced_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_replacement_course` (`course_id`),
  UNIQUE KEY `uk_course_replacement_course_spot` (`course_spot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_keyword` (
  `travel_room_id` bigint NOT NULL,
  `keyword_id` bigint NOT NULL,
  PRIMARY KEY (`travel_room_id`,`keyword_id`),
  KEY `idx_room_keyword_keyword` (`keyword_id`),
  CONSTRAINT `fk_room_keyword_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_room_keyword_travel_room` FOREIGN KEY (`travel_room_id`) REFERENCES `travel_room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방당 3~5건은 애플리케이션에서 검증(7.3절). 영구 보존';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `travel_room_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `is_host` tinyint(1) NOT NULL,
  `is_selection_completed` tinyint(1) NOT NULL DEFAULT '0',
  `completed_at` datetime DEFAULT NULL,
  `joined_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_participant` (`travel_room_id`,`member_id`),
  KEY `idx_room_participant_member` (`member_id`),
  CONSTRAINT `fk_room_participant_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_room_participant_travel_room` FOREIGN KEY (`travel_room_id`) REFERENCES `travel_room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='영구 보존(7.1절 보존정책 확정, 9차 검수)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invite_token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `host_member_id` bigint NOT NULL,
  `room_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `travel_date` date NOT NULL,
  `region_id` bigint NOT NULL,
  `course_spot_count` int NOT NULL,
  `includes_food` tinyint(1) NOT NULL,
  `includes_lodging` tinyint(1) NOT NULL,
  `includes_shopping` tinyint(1) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VOTING',
  `recommendation_condition_key` char(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '여행일+region_id+정렬한 키워드ID목록의 sha256 (7.2절)',
  `candidate_offset` int NOT NULL,
  `closed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_travel_room_invite_token` (`invite_token`),
  KEY `idx_travel_room_condition_key` (`recommendation_condition_key`),
  KEY `fk_travel_room_host_member` (`host_member_id`),
  KEY `fk_travel_room_region` (`region_id`),
  CONSTRAINT `fk_travel_room_host_member` FOREIGN KEY (`host_member_id`) REFERENCES `member` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_travel_room_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `travel_room_chk_1` CHECK ((`course_spot_count` between 3 and 6)),
  CONSTRAINT `travel_room_chk_2` CHECK ((`status` in (_utf8mb4'VOTING',_utf8mb4'CLOSED',_utf8mb4'COURSE_CONFIRMED')))
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='여행 한 번에 방 하나. 영구 보존(7.1절 보존정책 확정, 9차 검수)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_condition_counter` (
  `recommendation_condition_key` char(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AUTO_INCREMENT id를 두면 LAST_INSERT_ID(expr) 관용구가 깨짐(실측 확인, 7.7절)',
  `room_count` int NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`recommendation_condition_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='보존 정책 없음(7.7절) - 조회가 항상 PK 단건 조회뿐이라 실익이 낮음';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `travel_room_id` bigint NOT NULL,
  `content_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `keyword_id` bigint DEFAULT NULL COMMENT '어느 키워드로 뽑혔는지',
  `display_order` int NOT NULL,
  `recommendation_score` decimal(6,4) DEFAULT NULL COMMENT '0.6*crowdEase + 0.4*demandEase (9.5절)',
  `concentration_rate_snapshot` decimal(5,2) DEFAULT NULL,
  `concentration_grade_snapshot` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title_snapshot` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_url_snapshot` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude_snapshot` double NOT NULL,
  `longitude_snapshot` double NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_candidate` (`travel_room_id`,`content_id`),
  KEY `idx_room_candidate_order` (`travel_room_id`,`display_order`),
  KEY `idx_room_candidate_keyword` (`keyword_id`),
  KEY `idx_room_candidate_spot` (`content_id`),
  CONSTRAINT `fk_room_candidate_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_room_candidate_travel_room` FOREIGN KEY (`travel_room_id`) REFERENCES `travel_room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=367 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방 생성 시점 스냅샷으로 고정(7.5절). 영구 보존(7.1절 보존정책 확정, 9차 검수)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_vote` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_candidate_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `voted_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_vote` (`room_candidate_id`,`member_id`),
  KEY `idx_room_vote_member` (`member_id`),
  CONSTRAINT `fk_room_vote_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_room_vote_room_candidate` FOREIGN KEY (`room_candidate_id`) REFERENCES `room_candidate` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=118 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='투표 취소는 행 삭제(7.6절). 확정된(비취소) 투표 기록은 영구 보존(7.1절 보존정책 확정, 9차 검수)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keyword` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keyword_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_order` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='여행 키워드 12종';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keyword_classification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keyword_id` bigint NOT NULL,
  `classification_level2_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '분류체계 중분류 코드 (예: VE07)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword_classification` (`keyword_id`,`classification_level2_code`),
  CONSTRAINT `fk_keyword_classification_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='키워드 - 분류체계 중분류 매핑';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `area_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '한국관광공사 시도 코드 (서울=11)',
  `area_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `district_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시군구 코드 (예: 11110)',
  `district_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `legal_dong_area_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '법정동 시도 코드 (KorService2용)',
  `legal_dong_district_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '법정동 시군구 코드',
  `center_latitude` decimal(10,7) DEFAULT NULL,
  `center_longitude` decimal(10,7) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_area_district` (`area_code`,`district_code`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서울 25개 자치구';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seoul_realtime_area` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `area_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API 응답 영역 코드 (POI0xx)',
  `area_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `boundary` polygon NOT NULL /*!80003 SRID 4326 */ COMMENT '물리 설계 결정 1, 2 참고',
  `reference_population_max` int DEFAULT NULL COMMENT '배치 갱신, 최근 30일 area_ppltn_max 중앙값',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seoul_realtime_area_name` (`area_name`),
  SPATIAL KEY `idx_seoul_realtime_area_boundary` (`boundary`)
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서울 실시간 도시데이터 주요 121곳, 최초 1회 시딩';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seoul_realtime_population` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seoul_realtime_area_id` bigint NOT NULL,
  `congestion_level` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `congestion_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `population_min` int DEFAULT NULL,
  `population_max` int DEFAULT NULL,
  `resident_rate` decimal(5,2) DEFAULT NULL,
  `non_resident_rate` decimal(5,2) DEFAULT NULL,
  `observed_at` datetime NOT NULL,
  `collected_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_seoul_realtime_population_area_collected` (`seoul_realtime_area_id`,`collected_at`),
  CONSTRAINT `fk_seoul_realtime_population_area` FOREIGN KEY (`seoul_realtime_area_id`) REFERENCES `seoul_realtime_area` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `seoul_realtime_population_chk_1` CHECK (((`congestion_level` is null) or (`congestion_level` in (_utf8mb4'여유',_utf8mb4'보통',_utf8mb4'약간붐빔',_utf8mb4'붐빔'))))
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='30분 간격 적재, 30일 보존(배치 삭제), raw_json 미저장(5.4절)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

