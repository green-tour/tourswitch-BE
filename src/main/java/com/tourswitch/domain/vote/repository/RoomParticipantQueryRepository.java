package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * room_participant는 세션 도메인(조은혜)이 소유하는 테이블이라 JPA 엔티티로 매핑하지 않고
 * 네이티브 쿼리로 읽고 쓴다(B1 규칙). is_selection_completed 쓰기는 계획 문서 6절-3에서
 * 투표 도메인이 갱신 주체로 확인된 필드다.
 */
@Repository
public class RoomParticipantQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean isParticipant(Long travelRoomId, Long memberId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM room_participant WHERE travel_room_id = :travelRoomId AND member_id = :memberId
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    public boolean isHost(Long travelRoomId, Long memberId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM room_participant
                WHERE travel_room_id = :travelRoomId AND member_id = :memberId AND is_host = TRUE
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    public int updateSelectionCompletion(Long travelRoomId, Long memberId, boolean completed) {
        return entityManager.createNativeQuery("""
                UPDATE room_participant
                SET is_selection_completed = :completed, completed_at = :completedAt
                WHERE travel_room_id = :travelRoomId AND member_id = :memberId
                """)
                .setParameter("completed", completed)
                .setParameter("completedAt", completed ? LocalDateTime.now() : null)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    /**
     * FOR UPDATE로 읽는다. REPEATABLE READ(MySQL 기본 격리수준)의 트랜잭션 스냅샷만 읽으면,
     * 두 참여자가 거의 동시에 완료할 때 서로의 커밋을 못 보고 둘 다 "아직 미완료 있음"으로
     * 판단해 방이 영영 자동 종료되지 않는 경쟁 조건이 생길 수 있다. FOR UPDATE(락킹 리드)는
     * 스냅샷을 우회해 항상 최신 커밋 데이터를 읽으므로 이 문제를 막는다.
     */
    public boolean allParticipantsCompleted(Long travelRoomId) {
        Number incompleteCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM room_participant
                WHERE travel_room_id = :travelRoomId AND is_selection_completed = FALSE
                FOR UPDATE
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
        return hasParticipants(travelRoomId) && incompleteCount.longValue() == 0;
    }

    public int updateExtraSelectionCompletion(Long travelRoomId, Long memberId, boolean completed) {
        return entityManager.createNativeQuery("""
                UPDATE room_participant
                SET is_extra_selection_completed = :completed
                WHERE travel_room_id = :travelRoomId AND member_id = :memberId
                """)
                .setParameter("completed", completed)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    /**
     * 추가 투표 라운드의 전원 완료 판정. 1라운드와 같은 이유로 FOR UPDATE로 읽는다.
     */
    public boolean allParticipantsExtraCompleted(Long travelRoomId) {
        Number incompleteCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM room_participant
                WHERE travel_room_id = :travelRoomId AND is_extra_selection_completed = FALSE
                FOR UPDATE
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
        return hasParticipants(travelRoomId) && incompleteCount.longValue() == 0;
    }

    private boolean hasParticipants(Long travelRoomId) {
        return countParticipants(travelRoomId) > 0;
    }

    /**
     * 코스 확정 시 spot_daily_demand.participant_count 증분에 쓰인다(코스 도메인, DB설계 9.4절).
     */
    public long countParticipants(Long travelRoomId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM room_participant WHERE travel_room_id = :travelRoomId
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
        return count.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<ParticipantCompletionRow> findParticipantCompletions(Long travelRoomId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT rp.member_id, m.nickname, rp.is_selection_completed,
                       rp.is_extra_selection_completed, rp.completed_at
                FROM room_participant rp
                -- 회원 행이 사라져도 참여자가 목록에서 빠지면 완료 인원 계산이 어긋난다.
                LEFT JOIN member m ON m.id = rp.member_id
                WHERE rp.travel_room_id = :travelRoomId
                ORDER BY rp.is_host DESC, rp.joined_at, rp.id
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getResultList();

        return rows.stream()
                .map(row -> new ParticipantCompletionRow(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (Boolean) row[2],
                        (Boolean) row[3],
                        (LocalDateTime) row[4]))
                .toList();
    }
}
