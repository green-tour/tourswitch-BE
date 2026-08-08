package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

/**
 * travel_room은 세션 도메인(조은혜)이 소유하는 테이블이라 JPA 엔티티로 매핑하지 않고
 * 네이티브 쿼리로 읽고 쓴다(B1 규칙). 전원 완료 자동 종료(계획 문서 4단계)는 방장 수동 종료와
 * 경합할 수 있어, DB설계 7.4절의 조건부 UPDATE(WHERE status='VOTING')로 한쪽만 반영되게 한다.
 */
@Repository
public class TravelRoomStatusQueryRepository {

    private static final String VOTING_STATUS = "VOTING";
    private static final String CLOSED_STATUS = "CLOSED";

    @PersistenceContext
    private EntityManager entityManager;

    public String findStatus(Long travelRoomId) {
        return (String) entityManager.createNativeQuery("SELECT status FROM travel_room WHERE id = :travelRoomId")
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
    }

    public boolean closeIfVoting(Long travelRoomId) {
        int updated = entityManager.createNativeQuery("""
                UPDATE travel_room SET status = :closedStatus, closed_at = :closedAt
                WHERE id = :travelRoomId AND status = :votingStatus
                """)
                .setParameter("closedStatus", CLOSED_STATUS)
                .setParameter("closedAt", LocalDateTime.now())
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("votingStatus", VOTING_STATUS)
                .executeUpdate();
        return updated > 0;
    }
}
