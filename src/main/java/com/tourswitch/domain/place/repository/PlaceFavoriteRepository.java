package com.tourswitch.domain.place.repository;

import com.tourswitch.domain.place.entity.PlaceFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원별 관광지 찜의 저장과 조회를 담당한다.
 */
public interface PlaceFavoriteRepository extends JpaRepository<PlaceFavorite, Long> {

    /**
     * 회원의 찜을 최근 추가한 순서로 조회한다.
     */
    List<PlaceFavorite> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 회원이 특정 관광지를 찜했는지 확인한다.
     */
    boolean existsByMemberIdAndContentId(Long memberId, String contentId);

    /**
     * 회원과 관광지 식별자가 일치하는 찜을 조회한다.
     */
    Optional<PlaceFavorite> findByMemberIdAndContentId(Long memberId, String contentId);

    /**
     * 동시에 같은 찜 요청이 들어와도 유니크 키 충돌 없이 한 건만 저장한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO place_favorite (member_id, content_id, created_at)
            VALUES (:memberId, :contentId, NOW())
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId, @Param("contentId") String contentId);
}
