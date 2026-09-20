package com.tourswitch.domain.room.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveRoomQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<ActiveRoomRow> findLatestByMemberId(Long memberId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT tr.id, tr.room_name, tr.travel_date, tr.region_id, r.district_name, tr.status,
                       tr.host_member_id,
                       COUNT(all_rp.id),
                       COALESCE(SUM(CASE WHEN all_rp.is_selection_completed = TRUE THEN 1 ELSE 0 END), 0)
                FROM room_participant member_rp
                JOIN travel_room tr ON tr.id = member_rp.travel_room_id
                JOIN region r ON r.id = tr.region_id
                JOIN room_participant all_rp ON all_rp.travel_room_id = tr.id
                WHERE member_rp.member_id = :memberId
                  AND (
                      tr.status IN ('VOTING', 'EXTRA_VOTING')
                      OR (tr.status = 'COURSE_CONFIRMED' AND tr.travel_date >= CURRENT_DATE)
                  )
                GROUP BY tr.id, tr.room_name, tr.travel_date, tr.region_id, r.district_name, tr.status,
                         tr.host_member_id, tr.created_at
                ORDER BY tr.created_at DESC, tr.id DESC
                LIMIT 1
                """)
                .setParameter("memberId", memberId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new ActiveRoomRow(number(row[0]), (String) row[1], date(row[2]), number(row[3]),
                (String) row[4], (String) row[5], number(row[6]), count(row[7]), count(row[8])));
    }

    @SuppressWarnings("unchecked")
    public List<ActiveRoomKeywordRow> findKeywords(Long roomId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT k.id, k.keyword_name
                FROM room_keyword rk
                JOIN keyword k ON k.id = rk.keyword_id
                WHERE rk.travel_room_id = :roomId
                ORDER BY k.display_order ASC, k.id ASC
                """)
                .setParameter("roomId", roomId)
                .getResultList();
        return rows.stream().map(row -> new ActiveRoomKeywordRow(number(row[0]), (String) row[1])).toList();
    }

    private static Long number(Object value) {
        return ((Number) value).longValue();
    }

    private static long count(Object value) {
        return ((Number) value).longValue();
    }

    private static LocalDate date(Object value) {
        return value instanceof Date sqlDate ? sqlDate.toLocalDate() : (LocalDate) value;
    }
}
