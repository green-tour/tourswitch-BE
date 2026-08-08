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

    public boolean allParticipantsCompleted(Long travelRoomId) {
        Number incompleteCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM room_participant
                WHERE travel_room_id = :travelRoomId AND is_selection_completed = FALSE
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
                SELECT member_id, is_selection_completed, completed_at
                FROM room_participant WHERE travel_room_id = :travelRoomId
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getResultList();

        return rows.stream()
                .map(row -> new ParticipantCompletionRow(
                        ((Number) row[0]).longValue(),
                        (Boolean) row[1],
                        (LocalDateTime) row[2]))
                .toList();
    }
}
