package com.tourswitch.domain.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 후보 순환용 카운터. PK가 조건 키 자체다(계획 문서 3단계 - LAST_INSERT_ID(expr) 관용구 보호).
 */
@Entity
@Getter
@EqualsAndHashCode(of = "recommendationConditionKey")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "recommendation_condition_counter")
public class RecommendationConditionCounter {

    @Id
    @Column(name = "recommendation_condition_key", length = 64)
    private String recommendationConditionKey;

    @Column(name = "room_count", nullable = false)
    private Integer roomCount;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private RecommendationConditionCounter(String recommendationConditionKey) {
        this.recommendationConditionKey = recommendationConditionKey;
        this.roomCount = 0;
    }

    public static RecommendationConditionCounter create(String recommendationConditionKey) {
        return new RecommendationConditionCounter(recommendationConditionKey);
    }
}
