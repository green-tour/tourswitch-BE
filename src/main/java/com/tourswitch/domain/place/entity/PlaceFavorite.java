package com.tourswitch.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 찜한 TourAPI 관광지 식별자를 보관한다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "place_favorite", uniqueConstraints = {
        @UniqueConstraint(name = "uk_place_favorite_member_content", columnNames = {"member_id", "content_id"})
})
public class PlaceFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PlaceFavorite(Long memberId, String contentId) {
        this.memberId = memberId;
        this.contentId = contentId;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 회원과 TourAPI 관광지를 연결하는 찜 엔티티를 생성한다.
     */
    public static PlaceFavorite create(Long memberId, String contentId) {
        return new PlaceFavorite(memberId, contentId);
    }
}
