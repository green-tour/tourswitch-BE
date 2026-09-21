package com.tourswitch.domain.place.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * TourAPI 상세 관광지명과 집중률 서비스 관광지명의 수동 별칭 기준정보를 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class PlaceCrowdNameAliasQueryRepository {

    private final EntityManager entityManager;

    /**
     * 관광 콘텐츠 ID에 등록된 집중률 서비스용 관광지명 별칭을 반환한다.
     */
    @SuppressWarnings("unchecked")
    public List<String> findAttractionNamesByContentId(String contentId) {
        return entityManager.createNativeQuery("""
                SELECT crowd_attraction_name
                FROM place_crowd_name_alias
                WHERE content_id = :contentId
                ORDER BY crowd_attraction_name
                """)
                .setParameter("contentId", contentId)
                .getResultList();
    }
}
