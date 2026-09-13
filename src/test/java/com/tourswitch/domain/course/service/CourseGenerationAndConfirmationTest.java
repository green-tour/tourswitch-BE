package com.tourswitch.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.entity.CourseStatus;
import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.service.VoteService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전원 완료로 방이 자동 종료되면 코스가 자동 생성되고(방문 순서 최적화 포함), 방장 확정 시
 * spot_daily_demand가 원자적으로 갱신되며, 재확정은 멱등한지를 실제 좌표·스키마로 검증한다.
 * 동십자각(11)/서울 동관왕묘(12)/사직단(53)은 대략 동-서 일직선에 가깝게 위치해
 * (사직단-동십자각-동관왕묘 순, 서->동) 득표순 입력 순서([동관왕묘, 사직단, 동십자각])와
 * 최적 방문 순서가 달라지는 것을 확인하기 좋은 조합이다.
 */
@SpringBootTest
@Transactional
@Rollback
class CourseGenerationAndConfirmationTest {

    private static final Long REGION_ID = 1L;
    private static final Spot SPOT_DONGSIPJAGAK = new Spot("1603677", "동십자각", 37.5760791, 126.9794211);
    private static final Spot SPOT_DONGGWANWANGMYO = new Spot("126522", "서울 동관왕묘", 37.5727080, 127.0184229);
    private static final Spot SPOT_SAJIKDAN = new Spot("2992891", "사직단", 37.5759365, 126.9672638);
    private static final LocalDate TRAVEL_DATE = LocalDate.of(2026, 7, 28);

    private record Spot(String contentId, String title, double latitude, double longitude) {
    }

    @Autowired
    private VoteService voteService;

    @Autowired
    private CourseConfirmationService courseConfirmationService;

    @Autowired
    private CourseQueryService courseQueryService;

    @Autowired
    private RoomCandidateRepository roomCandidateRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 전원_완료시_코스가_최적순서로_자동생성되고_확정하면_수요가_반영된다() {
        Long hostMemberId = insertMember("smoke_test_course_host");
        Long member2Id = insertMember("smoke_test_course_member2");
        Long member3Id = insertMember("smoke_test_course_member3");
        Long travelRoomId = insertTravelRoom(hostMemberId);
        insertParticipant(travelRoomId, hostMemberId, true);
        insertParticipant(travelRoomId, member2Id, false);
        insertParticipant(travelRoomId, member3Id, false);

        // 득표순으로는 [동관왕묘(3표), 사직단(2표), 동십자각(1표)]가 되도록 후보/투표를 구성한다.
        Long candidateDonggwanwangmyo = createCandidate(travelRoomId, SPOT_DONGGWANWANGMYO, 1);
        Long candidateSajikdan = createCandidate(travelRoomId, SPOT_SAJIKDAN, 2);
        Long candidateDongsipjagak = createCandidate(travelRoomId, SPOT_DONGSIPJAGAK, 3);

        voteService.selectCandidate(travelRoomId, candidateDonggwanwangmyo, hostMemberId);
        voteService.selectCandidate(travelRoomId, candidateSajikdan, hostMemberId);
        voteService.selectCandidate(travelRoomId, candidateDongsipjagak, hostMemberId);

        voteService.selectCandidate(travelRoomId, candidateDonggwanwangmyo, member2Id);
        voteService.selectCandidate(travelRoomId, candidateSajikdan, member2Id);

        voteService.selectCandidate(travelRoomId, candidateDonggwanwangmyo, member3Id);

        voteService.completeSelection(travelRoomId, hostMemberId, true);
        voteService.completeSelection(travelRoomId, member2Id, true);
        voteService.completeSelection(travelRoomId, member3Id, true);

        Course draftCourse = courseQueryService.getCourseByTravelRoomId(travelRoomId, hostMemberId);
        assertThat(draftCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);

        List<CourseSpot> stops = courseQueryService.getStops(draftCourse.getId());
        assertThat(stops).hasSize(3);
        assertThat(stops).extracting(CourseSpot::getVisitOrder).containsExactly(1, 2, 3);

        List<String> naiveOrderSpotIds = List.of(SPOT_DONGGWANWANGMYO.contentId(), SPOT_SAJIKDAN.contentId(),
                SPOT_DONGSIPJAGAK.contentId());
        List<String> optimizedOrderSpotIds = stops.stream().map(CourseSpot::getContentId).toList();
        assertThat(optimizedOrderSpotIds).isNotEqualTo(naiveOrderSpotIds);
        assertThat(draftCourse.getTotalDistanceMeters()).isLessThan(5000);

        Course confirmedCourse = courseConfirmationService.confirmCourse(draftCourse.getId(), hostMemberId);
        assertThat(confirmedCourse.getStatus()).isEqualTo(CourseStatus.CONFIRMED);
        assertThat(findTravelRoomStatus(travelRoomId)).isEqualTo("COURSE_CONFIRMED");

        for (String contentId : optimizedOrderSpotIds) {
            assertThat(findParticipantCount(contentId, TRAVEL_DATE)).isEqualTo(3);
        }

        courseConfirmationService.confirmCourse(draftCourse.getId(), hostMemberId);
        for (String contentId : optimizedOrderSpotIds) {
            assertThat(findParticipantCount(contentId, TRAVEL_DATE)).isEqualTo(3);
        }
    }

    private Long createCandidate(Long travelRoomId, Spot spot, int displayOrder) {
        RoomCandidate candidate = RoomCandidate.create(travelRoomId, spot.contentId(), null, displayOrder, null,
                null, null, spot.title(), null, spot.latitude(), spot.longitude());
        return roomCandidateRepository.save(candidate).getId();
    }

    private String findTravelRoomStatus(Long travelRoomId) {
        return (String) entityManager.createNativeQuery("SELECT status FROM travel_room WHERE id = :id")
                .setParameter("id", travelRoomId)
                .getSingleResult();
    }

    private int findParticipantCount(String contentId, LocalDate targetDate) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT participant_count FROM spot_daily_demand
                WHERE content_id = :contentId AND target_date = :targetDate
                """)
                .setParameter("contentId", contentId)
                .setParameter("targetDate", targetDate)
                .getSingleResult();
        return count.intValue();
    }

    private Long insertMember(String socialId) {
        entityManager.createNativeQuery("""
                INSERT INTO member (social_provider, social_id, nickname, status, created_at)
                VALUES ('KAKAO', :socialId, '코스테스트', 'ACTIVE', NOW())
                """)
                .setParameter("socialId", socialId)
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private Long insertTravelRoom(Long hostMemberId) {
        entityManager.createNativeQuery("""
                INSERT INTO travel_room
                    (invite_token, host_member_id, room_name, travel_date, region_id, course_spot_count,
                     includes_food, includes_lodging, includes_shopping, status,
                     recommendation_condition_key, candidate_offset, created_at)
                VALUES ('smoke_test_course_invite_token', :hostMemberId, 'STAGE5_SMOKE_TEST', :travelDate,
                        :regionId, 3, FALSE, FALSE, FALSE, 'VOTING', REPEAT('2', 64), 0, NOW())
                """)
                .setParameter("hostMemberId", hostMemberId)
                .setParameter("travelDate", TRAVEL_DATE)
                .setParameter("regionId", REGION_ID)
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private void insertParticipant(Long travelRoomId, Long memberId, boolean isHost) {
        entityManager.createNativeQuery("""
                INSERT INTO room_participant (travel_room_id, member_id, is_host, is_selection_completed, joined_at)
                VALUES (:travelRoomId, :memberId, :isHost, FALSE, NOW())
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .setParameter("isHost", isHost)
                .executeUpdate();
    }
}
