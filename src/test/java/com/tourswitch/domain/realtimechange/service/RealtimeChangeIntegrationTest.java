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

    private static final Long JONGNO_REGION_ID = 1L;
    private static final Long CHEONGUN_HYOJA_DONG_ID = 1L;
    private static final Long MUSEUM_KEYWORD_ID = 1L;

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
        Long memberId = insertMember();
        Long travelRoomId = insertTravelRoom(memberId);
        insertParticipant(travelRoomId, memberId);
        insertRoomKeyword(travelRoomId, MUSEUM_KEYWORD_ID);

        Course course = Course.create(travelRoomId, LocalDate.now());
        course.confirm();
        courseRepository.save(course);

        Long originalSpotId = findOriginalSpotId();
        String originalTitle = findSpotTitle(originalSpotId);
        CourseSpot courseSpot = CourseSpot.create(course, originalSpotId, SpotRole.ATTRACTION, 1,
                originalTitle, BigDecimal.ZERO, 1);
        courseSpotRepository.save(courseSpot);
        entityManager.flush();

        ReplacementCandidatesResponseDTO response = realtimeChangeQueryService.getReplacementCandidates(
                course.getId(), CHEONGUN_HYOJA_DONG_ID, memberId, 20);

        assertThat(response.radiusMeters()).isEqualTo(3_000);
        assertThat(response.administrativeDong().dongName()).isEqualTo("청운효자동");
        assertThat(response.candidates()).isNotEmpty();
        assertThat(response.candidates()).allSatisfy(candidate -> {
            assertThat(candidate.distanceMeters()).isLessThanOrEqualTo(3_000);
            assertThat(candidate.matchedKeywords()).contains("전시·박물관");
            assertThat(candidate.touristSpotId()).isNotEqualTo(originalSpotId);
        });

        ReplacementCandidateResponseDTO selected = response.candidates().getFirst();
        CourseReplacementResponseDTO replacement = courseReplacementService.replace(
                course.getId(),
                courseSpot.getId(),
                memberId,
                new CourseSpotReplacementRequestDTO(CHEONGUN_HYOJA_DONG_ID, selected.touristSpotId()));

        entityManager.flush();
        entityManager.clear();

        CourseSpot replacedSpot = courseSpotRepository.findById(courseSpot.getId()).orElseThrow();
        assertThat(replacedSpot.getTouristSpotId()).isEqualTo(selected.touristSpotId());
        assertThat(replacedSpot.getReplacedFromSpotId()).isEqualTo(originalSpotId);
        assertThat(replacedSpot.getIsReplaced()).isTrue();
        assertThat(replacedSpot.getVoteCountSnapshot()).isNull();
        assertThat(replacement.radiusMeters()).isEqualTo(3_000);
        assertThat(courseReplacementRepository.existsByCourseId(course.getId())).isTrue();
    }

    private Long insertMember() {
        entityManager.createNativeQuery("""
                INSERT INTO member (login_id, password_hash, nickname, status, created_at)
                VALUES ('realtime_change_test_member', 'x', '실시간변경테스트', 'ACTIVE', NOW())
                """).executeUpdate();
        return lastInsertId();
    }

    private Long insertTravelRoom(Long hostMemberId) {
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
                .setParameter("regionId", JONGNO_REGION_ID)
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

    private Long findOriginalSpotId() {
        return ((Number) entityManager.createNativeQuery("""
                SELECT id FROM tourist_spot
                WHERE is_active = TRUE AND is_coordinate_valid = TRUE
                  AND content_type_id IN (12, 14, 15, 28)
                ORDER BY id LIMIT 1
                """).getSingleResult()).longValue();
    }

    private String findSpotTitle(Long touristSpotId) {
        return (String) entityManager.createNativeQuery("SELECT title FROM tourist_spot WHERE id = :id")
                .setParameter("id", touristSpotId)
                .getSingleResult();
    }

    private Long lastInsertId() {
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
