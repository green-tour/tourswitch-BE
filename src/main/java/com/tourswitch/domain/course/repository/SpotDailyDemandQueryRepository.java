package com.tourswitch.domain.course.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.springframework.stereotype.Repository;

/**
 * spot_daily_demand는 이 도메인이 소유한 엔티티(SpotDailyDemand)가 있지만, 코스 확정 시의
 * 증분 갱신은 동시 확정 경쟁에도 안전해야 해서 JPA 조회-후-갱신 대신 원자적 UPSERT를 쓴다
 * (계획 문서 5단계, DB설계 9.4절).
 */
@Repository
public class SpotDailyDemandQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void increment(Long touristSpotId, LocalDate targetDate, int participantCount) {
        entityManager.createNativeQuery("""
                INSERT INTO spot_daily_demand (tourist_spot_id, target_date, participant_count, course_count, updated_at)
                VALUES (:touristSpotId, :targetDate, :participantCount, 1, NOW())
                ON DUPLICATE KEY UPDATE
                    participant_count = participant_count + VALUES(participant_count),
                    course_count = course_count + 1,
                    updated_at = NOW()
                """)
                .setParameter("touristSpotId", touristSpotId)
                .setParameter("targetDate", targetDate)
                .setParameter("participantCount", participantCount)
                .executeUpdate();
    }
}
