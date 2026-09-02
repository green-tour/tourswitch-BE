package com.tourswitch.domain.room.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RoomReferenceQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public boolean memberExists(Long memberId) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM member WHERE id = :id")
                .setParameter("id", memberId).getSingleResult();
        return count.longValue() == 1;
    }

    public boolean regionExists(Long regionId) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM region WHERE id = :id")
                .setParameter("id", regionId).getSingleResult();
        return count.longValue() == 1;
    }

    public long countActiveKeywords(List<Long> keywordIds) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM keyword WHERE id IN (:ids) AND is_active = TRUE")
                .setParameter("ids", keywordIds).getSingleResult();
        return count.longValue();
    }
}
