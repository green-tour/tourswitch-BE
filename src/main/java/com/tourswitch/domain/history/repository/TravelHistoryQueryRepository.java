package com.tourswitch.domain.history.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TravelHistoryQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<TravelHistoryRow> findHistories(Long memberId, int page, int size) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT tr.id, c.id, tr.room_name, tr.travel_date, tr.region_id, r.district_name, tr.status,
                       (SELECT COUNT(*) FROM room_participant all_rp WHERE all_rp.travel_room_id = tr.id),
                       CASE WHEN c.id IS NULL THEN FALSE ELSE TRUE END,
                       CASE WHEN c.status = 'CONFIRMED' THEN TRUE ELSE FALSE END
                FROM room_participant rp
                JOIN travel_room tr ON tr.id = rp.travel_room_id
                JOIN region r ON r.id = tr.region_id
                LEFT JOIN course c ON c.travel_room_id = tr.id
                WHERE rp.member_id = :memberId
                  AND tr.status IN ('CLOSED', 'COURSE_CONFIRMED')
                ORDER BY tr.travel_date DESC, tr.id DESC
                LIMIT :size OFFSET :offset
                """)
                .setParameter("memberId", memberId)
                .setParameter("size", size)
                .setParameter("offset", page * size)
                .getResultList();

        return rows.stream().map(row -> new TravelHistoryRow(
                number(row[0]), nullableNumber(row[1]), (String) row[2], toLocalDate(row[3]), number(row[4]),
                (String) row[5], (String) row[6], ((Number) row[7]).longValue(), toBoolean(row[8]),
                toBoolean(row[9]))).toList();
    }

    public long countHistories(Long memberId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM room_participant rp
                JOIN travel_room tr ON tr.id = rp.travel_room_id
                WHERE rp.member_id = :memberId
                  AND tr.status IN ('CLOSED', 'COURSE_CONFIRMED')
                """).setParameter("memberId", memberId).getSingleResult();
        return count.longValue();
    }

    private static Long number(Object value) {
        return ((Number) value).longValue();
    }

    private static Long nullableNumber(Object value) {
        return value == null ? null : number(value);
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof Date date ? date.toLocalDate() : (LocalDate) value;
    }

    private static boolean toBoolean(Object value) {
        return value instanceof Boolean bool ? bool : ((Number) value).intValue() != 0;
    }
}
