package com.tourswitch.domain.realtimechange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 여행 당일 한 장소 교체 이력.
 *
 * <p>course_id 유니크 제약으로 코스 전체에서 한 번만 교체할 수 있게 한다. 코스의 현재 상태는
 * course_spot이 소유하고, 이 엔티티는 선택 위치와 교체 전후 장소를 감사 이력으로 보존한다.</p>
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course_replacement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_replacement_course", columnNames = "course_id"),
        @UniqueConstraint(name = "uk_course_replacement_course_spot", columnNames = "course_spot_id")
})
public class CourseReplacement {

    public static final int SEARCH_RADIUS_METERS = 3_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_spot_id", nullable = false)
    private Long courseSpotId;

    @Column(name = "administrative_dong_id", nullable = false)
    private Long administrativeDongId;

    @Column(name = "previous_tourist_spot_id", nullable = false)
    private Long previousTouristSpotId;

    @Column(name = "replacement_tourist_spot_id", nullable = false)
    private Long replacementTouristSpotId;

    @Column(name = "replaced_by_member_id", nullable = false)
    private Long replacedByMemberId;

    @Column(name = "radius_meters", nullable = false)
    private Integer radiusMeters;

    @CreatedDate
    @Column(name = "replaced_at", nullable = false, updatable = false)
    private LocalDateTime replacedAt;

    private CourseReplacement(Long courseId, Long courseSpotId, Long administrativeDongId,
                              Long previousTouristSpotId, Long replacementTouristSpotId,
                              Long replacedByMemberId) {
        if (previousTouristSpotId.equals(replacementTouristSpotId)) {
            throw new IllegalArgumentException("기존 장소와 대체 장소는 달라야 합니다.");
        }
        this.courseId = courseId;
        this.courseSpotId = courseSpotId;
        this.administrativeDongId = administrativeDongId;
        this.previousTouristSpotId = previousTouristSpotId;
        this.replacementTouristSpotId = replacementTouristSpotId;
        this.replacedByMemberId = replacedByMemberId;
        this.radiusMeters = SEARCH_RADIUS_METERS;
    }

    public static CourseReplacement create(Long courseId, Long courseSpotId, Long administrativeDongId,
                                           Long previousTouristSpotId, Long replacementTouristSpotId,
                                           Long replacedByMemberId) {
        return new CourseReplacement(courseId, courseSpotId, administrativeDongId, previousTouristSpotId,
                replacementTouristSpotId, replacedByMemberId);
    }
}
