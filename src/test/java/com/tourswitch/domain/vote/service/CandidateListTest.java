package com.tourswitch.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.response.CandidateGroupResponseDTO;
import com.tourswitch.domain.vote.response.CandidateListResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /api/rooms/{roomId}/candidates가 room_candidate를 키워드별로 묶고, tourist_spot 상세와
 * 내 투표 여부(myVote)를 실제 스키마로 정확히 채우는지 검증한다.
 */
@SpringBootTest
@Transactional
@Rollback
class CandidateListTest {

    private static final Long TOURIST_SPOT_ID = 11L; // 동십자각 (region_id=1)

    @Autowired
    private VoteService voteService;

    @Autowired
    private RoomCandidateRepository roomCandidateRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 키워드별로_묶고_내_투표_여부를_채워_후보목록을_반환한다() {
        Long memberId = insertMember();
        Long travelRoomId = insertTravelRoom(memberId);
        insertParticipant(travelRoomId, memberId);
        RoomCandidate candidate = roomCandidateRepository.save(
                RoomCandidate.create(travelRoomId, TOURIST_SPOT_ID, 4L, 1, null, null, null));

        CandidateListResponseDTO before = voteService.getCandidateList(travelRoomId, memberId);
        assertThat(before.totalCount()).isEqualTo(1);
        CandidateGroupResponseDTO group = before.candidateGroups().get(0);
        assertThat(group.keywordId()).isEqualTo(4L);
        assertThat(group.keywordName()).isEqualTo("역사유적");
        assertThat(group.items()).singleElement().satisfies(item -> {
            assertThat(item.touristSpotId()).isEqualTo(TOURIST_SPOT_ID);
            assertThat(item.title()).isEqualTo("동십자각");
            assertThat(item.myVote()).isFalse();
        });

        voteService.selectCandidate(travelRoomId, candidate.getId(), memberId);

        CandidateListResponseDTO after = voteService.getCandidateList(travelRoomId, memberId);
        assertThat(after.candidateGroups().get(0).items().get(0).myVote()).isTrue();
    }

    private Long insertMember() {
        entityManager.createNativeQuery("""
                INSERT INTO member (login_id, password_hash, nickname, status, created_at)
                VALUES ('smoke_test_candidate_list', 'x', '후보목록테스트', 'ACTIVE', NOW())
                """).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private Long insertTravelRoom(Long hostMemberId) {
        entityManager.createNativeQuery("""
                INSERT INTO travel_room
                    (invite_token, host_member_id, room_name, travel_date, region_id, course_spot_count,
                     includes_food, includes_lodging, includes_shopping, status,
                     recommendation_condition_key, candidate_offset, created_at)
                VALUES ('smoke_test_candidate_list_token', :hostMemberId, 'CANDIDATE_LIST_TEST',
                        :travelDate, 1, 3, FALSE, FALSE, FALSE, 'VOTING', REPEAT('3', 64), 0, NOW())
                """)
                .setParameter("hostMemberId", hostMemberId)
                .setParameter("travelDate", LocalDate.of(2026, 7, 28))
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private void insertParticipant(Long travelRoomId, Long memberId) {
        entityManager.createNativeQuery("""
                INSERT INTO room_participant (travel_room_id, member_id, is_host, is_selection_completed, joined_at)
                VALUES (:travelRoomId, :memberId, TRUE, FALSE, NOW())
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }
}
