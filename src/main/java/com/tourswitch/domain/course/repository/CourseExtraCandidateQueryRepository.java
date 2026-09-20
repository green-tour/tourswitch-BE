package com.tourswitch.domain.course.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 추가 투표 라운드에서 쓰는 조회. 방 기준으로 부가 후보와 득표를 함께 읽어야 해서
 * 코스 도메인 엔티티 그래프를 타지 않고 네이티브 쿼리로 처리한다.
 */
@Repository
public class CourseExtraCandidateQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean existsByTravelRoomId(Long travelRoomId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM course_extra_candidate cec
                JOIN course c ON c.id = cec.course_id
                WHERE c.travel_room_id = :travelRoomId
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 후보 목록과 득표 수, 그리고 요청한 참여자가 고른 항목인지를 함께 돌려준다.
     */
    @SuppressWarnings("unchecked")
    public List<ExtraCandidateRow> findByTravelRoomId(Long travelRoomId, Long memberId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT cec.id, cec.spot_role, cec.content_id, cec.title_snapshot, cec.image_url_snapshot,
                       cec.distance_meters, cec.display_order, cec.is_selected,
                       (SELECT COUNT(*) FROM course_extra_vote v WHERE v.course_extra_candidate_id = cec.id),
                       (SELECT COUNT(*) FROM course_extra_vote v
                         WHERE v.course_extra_candidate_id = cec.id AND v.member_id = :memberId)
                FROM course_extra_candidate cec
                JOIN course c ON c.id = cec.course_id
                WHERE c.travel_room_id = :travelRoomId
                ORDER BY cec.spot_role, cec.display_order
                """)
                .setParameter("travelRoomId", travelRoomId)
                .setParameter("memberId", memberId)
                .getResultList();
        return rows.stream()
                .map(row -> new ExtraCandidateRow(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        ((Number) row[5]).intValue(),
                        ((Number) row[6]).intValue(),
                        toBoolean(row[7]),
                        ((Number) row[8]).longValue(),
                        ((Number) row[9]).longValue() > 0))
                .toList();
    }

    public boolean belongsToRoom(Long candidateId, Long travelRoomId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM course_extra_candidate cec
                JOIN course c ON c.id = cec.course_id
                WHERE cec.id = :candidateId AND c.travel_room_id = :travelRoomId
                """)
                .setParameter("candidateId", candidateId)
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 역할별로 최다 득표 후보 하나를 고른다. 동점이거나 아무도 고르지 않았으면 가장 가까운 곳을 쓴다.
     */
    @SuppressWarnings("unchecked")
    public List<Long> findWinnerIdsByRole(Long travelRoomId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT cec.spot_role, cec.id,
                       (SELECT COUNT(*) FROM course_extra_vote v WHERE v.course_extra_candidate_id = cec.id) AS votes
                FROM course_extra_candidate cec
                JOIN course c ON c.id = cec.course_id
                WHERE c.travel_room_id = :travelRoomId
                ORDER BY cec.spot_role, votes DESC, cec.distance_meters ASC, cec.id ASC
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getResultList();
        List<Long> winners = new java.util.ArrayList<>();
        String previousRole = null;
        for (Object[] row : rows) {
            String role = (String) row[0];
            if (role.equals(previousRole)) {
                continue;
            }
            previousRole = role;
            // 아무도 고르지 않은 역할은 거리순으로 임의 확정하지 않고 비워 둔다.
            if (((Number) row[2]).longValue() == 0L) {
                continue;
            }
            winners.add(((Number) row[1]).longValue());
        }
        return winners;
    }

    private static boolean toBoolean(Object value) {
        return value instanceof Boolean bool ? bool : ((Number) value).intValue() != 0;
    }

    public record ExtraCandidateRow(Long id, String spotRole, String contentId, String title, String imageUrl,
                                     int distanceMeters, int displayOrder, boolean isSelected, long voteCount,
                                     boolean myVote) {
    }
}
