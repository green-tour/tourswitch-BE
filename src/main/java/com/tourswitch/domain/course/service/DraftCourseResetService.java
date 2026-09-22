package com.tourswitch.domain.course.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재투표가 시작될 때만 아직 확정되지 않은 코스 초안과 그에 딸린 추가 투표 정보를 지운다.
 * 확정 코스는 이 서비스를 통해 삭제하지 않는다.
 */
@Service
public class DraftCourseResetService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void deleteDraftForRoom(Long travelRoomId) {
        Number draftCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM course
                WHERE travel_room_id = :travelRoomId AND status = 'DRAFT'
                """)
                .setParameter("travelRoomId", travelRoomId)
                .getSingleResult();
        if (draftCount.longValue() == 0) {
            return;
        }

        entityManager.createNativeQuery("""
                DELETE cev FROM course_extra_vote cev
                JOIN course_extra_candidate cec ON cec.id = cev.course_extra_candidate_id
                JOIN course c ON c.id = cec.course_id
                WHERE c.travel_room_id = :travelRoomId AND c.status = 'DRAFT'
                """).setParameter("travelRoomId", travelRoomId).executeUpdate();
        entityManager.createNativeQuery("""
                DELETE cec FROM course_extra_candidate cec
                JOIN course c ON c.id = cec.course_id
                WHERE c.travel_room_id = :travelRoomId AND c.status = 'DRAFT'
                """).setParameter("travelRoomId", travelRoomId).executeUpdate();
        entityManager.createNativeQuery("""
                DELETE cr FROM course_replacement cr
                JOIN course c ON c.id = cr.course_id
                WHERE c.travel_room_id = :travelRoomId AND c.status = 'DRAFT'
                """).setParameter("travelRoomId", travelRoomId).executeUpdate();
        entityManager.createNativeQuery("""
                DELETE cs FROM course_spot cs
                JOIN course c ON c.id = cs.course_id
                WHERE c.travel_room_id = :travelRoomId AND c.status = 'DRAFT'
                """).setParameter("travelRoomId", travelRoomId).executeUpdate();
        entityManager.createNativeQuery("""
                DELETE FROM course
                WHERE travel_room_id = :travelRoomId AND status = 'DRAFT'
                """).setParameter("travelRoomId", travelRoomId).executeUpdate();
    }
}
