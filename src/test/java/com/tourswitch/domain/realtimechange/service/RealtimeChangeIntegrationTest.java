package com.tourswitch.domain.realtimechange.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.entity.SpotRole;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import com.tourswitch.domain.realtimechange.repository.CourseReplacementRepository;
import com.tourswitch.domain.realtimechange.request.CourseSpotReplacementRequestDTO;
import com.tourswitch.domain.realtimechange.response.CourseReplacementResponseDTO;
import com.tourswitch.domain.realtimechange.response.ReplacementCandidateResponseDTO;
import com.tourswitch.domain.realtimechange.response.ReplacementCandidatesResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback
class RealtimeChangeIntegrationTest {

    @Autowired
    private RealtimeChangeQueryService realtimeChangeQueryService;

    @Autowired
    private CourseReplacementService courseReplacementService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSpotRepository courseSpotRepository;

    @Autowired
    private CourseReplacementRepository courseReplacementRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 여행방_키워드와_동_기준_3km로_후보를_조회하고_한_장소를_교체한다() {
        Long regionId = findRegionId();
        Long keywordId = findKeywordId();
        Long administrativeDongId = insertAdministrativeDong(regionId);
        insertReplacementCandidate(regionId, keywordId);
        Long memberId = insertMember();
        Long travelRoomId = insertTravelRoom(memberId, regionId);
        insertParticipant(travelRoomId, memberId);
        insertRoomKeyword(travelRoomId, keywordId);

        Course course = Course.create(travelRoomId, LocalDate.now());
        course.confirm();
        courseRepository.save(course);

        String originalContentId = "test-original-content-id";
        String originalTitle = "기존 테스트 장소";
        CourseSpot courseSpot = CourseSpot.create(course, originalContentId, SpotRole.ATTRACTION, 1,
                originalTitle, BigDecimal.ZERO, 1);
        courseSpotRepository.save(courseSpot);
        entityManager.flush();

        ReplacementCandidatesResponseDTO response = realtimeChangeQueryService.getReplacementCandidates(
                course.getId(), administrativeDongId, memberId, 20);

        assertThat(response.radiusMeters()).isEqualTo(3_000);
        assertThat(response.administrativeDong().dongName()).isEqualTo("청운효자동");
        assertThat(response.candidates()).isNotEmpty();
        assertThat(response.candidates()).allSatisfy(candidate -> {
            assertThat(candidate.distanceMeters()).isLessThanOrEqualTo(3_000);
            assertThat(candidate.matchedKeywords()).contains("전시·박물관");
            assertThat(candidate.contentId()).isNotEqualTo(originalContentId);
        });

        ReplacementCandidateResponseDTO selected = response.candidates().getFirst();
        CourseReplacementResponseDTO replacement = courseReplacementService.replace(
                course.getId(),
                courseSpot.getId(),
                memberId,
                new CourseSpotReplacementRequestDTO(administrativeDongId, selected.contentId()));

        entityManager.flush();
        entityManager.clear();

        CourseSpot replacedSpot = courseSpotRepository.findById(courseSpot.getId()).orElseThrow();
        assertThat(replacedSpot.getContentId()).isEqualTo(selected.contentId());
        assertThat(replacedSpot.getReplacedFromSpotId()).isEqualTo(originalContentId);
        assertThat(replacedSpot.getIsReplaced()).isTrue();
        assertThat(replacedSpot.getVoteCountSnapshot()).isNull();
        assertThat(replacement.radiusMeters()).isEqualTo(3_000);
        assertThat(courseReplacementRepository.existsByCourseId(course.getId())).isTrue();
    }

    private Long insertMember() {
        entityManager.createNativeQuery("""
                INSERT INTO member (social_provider, social_id, nickname, status, created_at)
                VALUES ('KAKAO', 'realtime_change_test_member', '실시간변경테스트', 'ACTIVE', NOW())
                """).executeUpdate();
        return lastInsertId();
    }

    private Long insertTravelRoom(Long hostMemberId, Long regionId) {
        entityManager.createNativeQuery("""
                INSERT INTO travel_room
                    (invite_token, host_member_id, room_name, travel_date, region_id, course_spot_count,
                     includes_food, includes_lodging, includes_shopping, status,
                     recommendation_condition_key, candidate_offset, created_at)
                VALUES ('realtime_change_test_invite', :hostMemberId, '실시간 변경 테스트', :travelDate,
                        :regionId, 3, FALSE, FALSE, FALSE, 'COURSE_CONFIRMED', REPEAT('7', 64), 0, NOW())
                """)
                .setParameter("hostMemberId", hostMemberId)
                .setParameter("travelDate", LocalDate.now())
                .setParameter("regionId", regionId)
                .executeUpdate();
        return lastInsertId();
    }

    private void insertParticipant(Long travelRoomId, Long memberId) {
        entityManager.createNativeQuery("""
                INSERT INTO room_participant
                    (travel_room_id, member_id, is_host, is_selection_completed, joined_at)
                VALUES (:travelRoomId, :memberId, TRUE, TRUE, NOW())
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    private void insertRoomKeyword(Long travelRoomId, Long keywordId) {
        entityManager.createNativeQuery("""
                INSERT INTO room_keyword (travel_room_id, keyword_id)
                VALUES (:travelRoomId, :keywordId)
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("keywordId", keywordId)
                .executeUpdate();
    }

    private Long findRegionId() {
        return ((Number) entityManager.createNativeQuery("""
                SELECT id FROM region WHERE district_code = '11110'
                """).getSingleResult()).longValue();
    }

    private Long findKeywordId() {
        return ((Number) entityManager.createNativeQuery("""
                SELECT id FROM keyword WHERE keyword_name = '전시·박물관'
                """).getSingleResult()).longValue();
    }

    private Long insertAdministrativeDong(Long regionId) {
        entityManager.createNativeQuery("""
                INSERT INTO administrative_dong
                  (region_id, dong_code, dong_name, center_latitude, center_longitude, is_active)
                VALUES (:regionId, 'realtime-test-dong', '청운효자동', 37.5840, 126.9707, TRUE)
                """)
                .setParameter("regionId", regionId)
                .executeUpdate();
        return lastInsertId();
    }

    private void insertReplacementCandidate(Long regionId, Long keywordId) {
        entityManager.createNativeQuery("""
                INSERT INTO tourist_spot
                  (content_id, content_type_id, title, normalized_title, address, normalized_address,
                   latitude, longitude, location_point, classification_level1_code,
                   classification_level2_code, classification_level3_code, region_id,
                   is_coordinate_valid, has_crowd_data, is_active, data_synced_at)
                VALUES ('realtime-replacement-candidate', 14, '청운 미술관', '청운미술관',
                        '서울 종로구 청운동', '서울종로구청운동', 37.5845, 126.9710,
                        ST_GeomFromText('POINT(126.9710 37.5845)', 4326, 'axis-order=long-lat'),
                        'VE', 'VE07', 'VE0701', :regionId, TRUE, FALSE, TRUE, UTC_TIMESTAMP())
                """)
                .setParameter("regionId", regionId)
                .executeUpdate();
        Long touristSpotId = lastInsertId();
        entityManager.createNativeQuery("""
                INSERT INTO spot_keyword_link (tourist_spot_id, keyword_id)
                VALUES (:touristSpotId, :keywordId)
                """)
                .setParameter("touristSpotId", touristSpotId)
                .setParameter("keywordId", keywordId)
                .executeUpdate();
    }

    private Long lastInsertId() {
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
