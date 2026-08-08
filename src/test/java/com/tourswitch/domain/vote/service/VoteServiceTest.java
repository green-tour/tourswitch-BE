package com.tourswitch.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.exception.VoteSessionNotActiveException;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.response.CandidateTallyResponseDTO;
import com.tourswitch.domain.vote.response.VoteTallyResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선택-취소 멱등성, 참여자 전원(1명) 완료 시 자동 종료, 종료 후 요청 거부를 실제 스키마로 검증한다.
 * 트랜잭션 롤백으로 정리하므로 생성한 행은 남지 않는다.
 */
@SpringBootTest
@Transactional
@Rollback
class VoteServiceTest {

    private static final Long TOURIST_SPOT_ID = 11L;

    @Autowired
    private VoteService voteService;

    @Autowired
    private RoomCandidateRepository roomCandidateRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 선택_취소는_멱등하고_유일_참여자가_완료하면_방이_자동_종료된다() {
        Long memberId = insertTestMember();
        Long travelRoomId = insertTestTravelRoom();
        insertTestParticipant(travelRoomId, memberId);
        Long candidateId = roomCandidateRepository.save(
                RoomCandidate.create(travelRoomId, TOURIST_SPOT_ID, null, 1, null, null, null)).getId();

        VoteTallyResponseDTO afterFirstSelect = voteService.selectCandidate(travelRoomId, candidateId, memberId);
        assertThat(voteCountOf(afterFirstSelect, candidateId)).isEqualTo(1);

        VoteTallyResponseDTO afterDuplicateSelect = voteService.selectCandidate(travelRoomId, candidateId, memberId);
        assertThat(voteCountOf(afterDuplicateSelect, candidateId)).isEqualTo(1);

        VoteTallyResponseDTO afterCancel = voteService.cancelVote(travelRoomId, candidateId, memberId);
        assertThat(voteCountOf(afterCancel, candidateId)).isEqualTo(0);

        VoteTallyResponseDTO afterDuplicateCancel = voteService.cancelVote(travelRoomId, candidateId, memberId);
        assertThat(voteCountOf(afterDuplicateCancel, candidateId)).isEqualTo(0);

        voteService.selectCandidate(travelRoomId, candidateId, memberId);

        VoteTallyResponseDTO afterCompletion = voteService.completeSelection(travelRoomId, memberId, true);
        assertThat(afterCompletion.roomStatus()).isEqualTo("CLOSED");
        assertThat(afterCompletion.participants()).singleElement()
                .satisfies(participant -> assertThat(participant.completed()).isTrue());

        assertThatThrownBy(() -> voteService.selectCandidate(travelRoomId, candidateId, memberId))
                .isInstanceOf(VoteSessionNotActiveException.class);
    }

    private long voteCountOf(VoteTallyResponseDTO tally, Long candidateId) {
        return tally.candidates().stream()
                .filter(candidate -> candidate.candidateId().equals(candidateId))
                .findFirst()
                .map(CandidateTallyResponseDTO::voteCount)
                .orElseThrow();
    }

    private Long insertTestMember() {
        entityManager.createNativeQuery("""
                INSERT INTO member (login_id, password_hash, nickname, status, created_at)
                VALUES ('smoke_test_vote_member', 'x', '투표테스트', 'ACTIVE', NOW())
                """).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private Long insertTestTravelRoom() {
        entityManager.createNativeQuery("""
                INSERT INTO travel_room
                    (invite_token, host_member_id, room_name, travel_date, region_id, course_spot_count,
                     includes_food, includes_lodging, includes_shopping, status,
                     recommendation_condition_key, candidate_offset, created_at)
                SELECT 'smoke_test_vote_invite_token', id, 'STAGE4_SMOKE_TEST', :travelDate, 1, 3,
                       FALSE, FALSE, FALSE, 'VOTING', REPEAT('1', 64), 0, NOW()
                FROM member WHERE login_id = 'smoke_test_vote_member'
                """)
                .setParameter("travelDate", LocalDate.of(2026, 7, 28))
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private void insertTestParticipant(Long travelRoomId, Long memberId) {
        entityManager.createNativeQuery("""
                INSERT INTO room_participant
                    (travel_room_id, member_id, is_host, is_selection_completed, joined_at)
                VALUES (:travelRoomId, :memberId, TRUE, FALSE, NOW())
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }
}
