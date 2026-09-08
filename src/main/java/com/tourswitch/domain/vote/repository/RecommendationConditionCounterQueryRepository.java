package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * candidate_offset 원자적 배정(계획 문서 3단계, DB설계 7.7절 LAST_INSERT_ID(expr) 관용구).
 * 방 생성 트랜잭션 안에서 호출되어야 하므로 자체 @Transactional을 걸지 않고 호출자의 트랜잭션을 그대로 탄다.
 */
@Repository
public class RecommendationConditionCounterQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int incrementAndGetRawOffset(String conditionKey) {
        entityManager.createNativeQuery("""
                INSERT INTO recommendation_condition_counter
                    (recommendation_condition_key, room_count, updated_at)
                VALUES (:key, LAST_INSERT_ID(0) + 1, NOW())
                ON DUPLICATE KEY UPDATE
                    room_count = LAST_INSERT_ID(room_count) + 1,
                    updated_at = NOW()
                """)
                .setParameter("key", conditionKey)
                .executeUpdate();

        Number rawOffset = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult();
        return rawOffset.intValue();
    }
}
