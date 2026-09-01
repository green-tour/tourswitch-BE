package com.tourswitch.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 경유지. 당일 교체 규약(스냅샷 비움, is_replaced 등)은 교체 기능(도진희 담당) 구현 시점에
 * 전용 메서드를 추가한다 - 지금은 생성 시점 필드만 다룬다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_spot", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_spot", columnNames = {"course_id", "visit_order"})
})
public class CourseSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "tourist_spot_id", nullable = false)
    private Long touristSpotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_role", nullable = false, length = 20)
    private SpotRole spotRole;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "spot_title_snapshot", nullable = false, length = 200)
    private String spotTitleSnapshot;

    @Column(name = "concentration_rate_snapshot", precision = 5, scale = 2)
    private BigDecimal concentrationRateSnapshot;

    @Column(name = "vote_count_snapshot")
    private Integer voteCountSnapshot;

    @Column(name = "is_replaced", nullable = false)
    private Boolean isReplaced;

    @Column(name = "replaced_from_spot_id")
    private Long replacedFromSpotId;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    private CourseSpot(Course course, Long touristSpotId, SpotRole spotRole, Integer visitOrder,
                        String spotTitleSnapshot, BigDecimal concentrationRateSnapshot, Integer voteCountSnapshot) {
        this.course = course;
        this.touristSpotId = touristSpotId;
        this.spotRole = spotRole;
        this.visitOrder = visitOrder;
        this.spotTitleSnapshot = spotTitleSnapshot;
        this.concentrationRateSnapshot = concentrationRateSnapshot;
        this.voteCountSnapshot = voteCountSnapshot;
        this.isReplaced = false;
    }

    public static CourseSpot create(Course course, Long touristSpotId, SpotRole spotRole, Integer visitOrder,
                                     String spotTitleSnapshot, BigDecimal concentrationRateSnapshot,
                                     Integer voteCountSnapshot) {
        return new CourseSpot(course, touristSpotId, spotRole, visitOrder, spotTitleSnapshot,
                concentrationRateSnapshot, voteCountSnapshot);
    }

    /**
     * 여행 당일 대체 장소를 현재 경유지에 반영한다. 투표를 거치지 않은 장소이므로 기존 투표·집중률
     * 스냅샷은 새 장소에 유효하지 않아 비운다.
     */
    public void replaceWith(Long replacementTouristSpotId, String replacementSpotTitle,
                            LocalDateTime replacementTime) {
        if (Boolean.TRUE.equals(isReplaced)) {
            throw new IllegalStateException("이미 교체된 코스 장소입니다.");
        }
        this.replacedFromSpotId = this.touristSpotId;
        this.touristSpotId = replacementTouristSpotId;
        this.spotTitleSnapshot = replacementSpotTitle;
        this.concentrationRateSnapshot = null;
        this.voteCountSnapshot = null;
        this.isReplaced = true;
        this.replacedAt = replacementTime;
    }
}
