package com.tourswitch.domain.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * keyword_classification은 TourAPI 응답이 아니라 우리가 만든 키워드-분류체계 매핑 규칙이라
 * 로컬 보관 대상이다(TourAPI 실시간전환 계획 문서 2절). areaBasedList2 호출 시 lclsSystm2
 * 필터 값으로 쓴다.
 */
@Repository
public class KeywordClassificationQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<String> findClassificationLevel2CodesByKeywordId(Long keywordId) {
        return entityManager.createNativeQuery("""
                SELECT classification_level2_code
                FROM keyword_classification
                WHERE keyword_id = :keywordId
                """)
                .setParameter("keywordId", keywordId)
                .getResultList();
    }
}
