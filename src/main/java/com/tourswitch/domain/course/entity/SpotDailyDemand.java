package com.tourswitch.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 날짜별 내부 수요 집계. 코스 CONFIRMED 시점에 원자적 UPSERT로 갱신한다(계획 문서 5단계).
 * 실제 증분 갱신은 QueryRepository의 네이티브 UPSERT로 하므로, 여기서는 필드만 다룬다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "spot_daily_demand", uniqueConstraints = {
        @UniqueConstraint(name = "uk_spot_daily_demand", columnNames = {"tourist_spot_id", "target_date"})
})
public class SpotDailyDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tourist_spot_id", nullable = false)
    private Long touristSpotId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(name = "course_count", nullable = false)
    private Integer courseCount;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SpotDailyDemand(Long touristSpotId, LocalDate targetDate) {
        this.touristSpotId = touristSpotId;
        this.targetDate = targetDate;
        this.participantCount = 0;
        this.courseCount = 0;
    }

    public static SpotDailyDemand create(Long touristSpotId, LocalDate targetDate) {
        return new SpotDailyDemand(touristSpotId, targetDate);
    }
}
