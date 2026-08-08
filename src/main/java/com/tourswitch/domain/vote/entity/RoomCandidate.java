package com.tourswitch.domain.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 방 생성 시점에 고정되는 후보 카드 스냅샷. 생성 이후 필드는 바뀌지 않는다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "room_candidate", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_candidate", columnNames = {"travel_room_id", "tourist_spot_id"})
})
public class RoomCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "travel_room_id", nullable = false)
    private Long travelRoomId;

    @Column(name = "tourist_spot_id", nullable = false)
    private Long touristSpotId;

    @Column(name = "keyword_id")
    private Long keywordId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "recommendation_score", precision = 6, scale = 4)
    private BigDecimal recommendationScore;

    @Column(name = "concentration_rate_snapshot", precision = 5, scale = 2)
    private BigDecimal concentrationRateSnapshot;

    @Column(name = "concentration_grade_snapshot", length = 10)
    private String concentrationGradeSnapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RoomCandidate(Long travelRoomId, Long touristSpotId, Long keywordId, Integer displayOrder,
                           BigDecimal recommendationScore, BigDecimal concentrationRateSnapshot,
                           String concentrationGradeSnapshot) {
        this.travelRoomId = travelRoomId;
        this.touristSpotId = touristSpotId;
        this.keywordId = keywordId;
        this.displayOrder = displayOrder;
        this.recommendationScore = recommendationScore;
        this.concentrationRateSnapshot = concentrationRateSnapshot;
        this.concentrationGradeSnapshot = concentrationGradeSnapshot;
    }

    public static RoomCandidate create(Long travelRoomId, Long touristSpotId, Long keywordId, Integer displayOrder,
                                        BigDecimal recommendationScore, BigDecimal concentrationRateSnapshot,
                                        String concentrationGradeSnapshot) {
        return new RoomCandidate(travelRoomId, touristSpotId, keywordId, displayOrder,
                recommendationScore, concentrationRateSnapshot, concentrationGradeSnapshot);
    }
}
