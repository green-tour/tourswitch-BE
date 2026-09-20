package com.tourswitch.domain.course.entity;

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
 * 추가 투표 라운드에서 참여자가 고른 부가 후보. 한 명이 같은 후보에 한 표만 던진다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_extra_vote", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_extra_vote",
                columnNames = {"course_extra_candidate_id", "member_id"})
})
public class CourseExtraVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_extra_candidate_id", nullable = false)
    private Long courseExtraCandidateId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CourseExtraVote(Long courseExtraCandidateId, Long memberId) {
        this.courseExtraCandidateId = courseExtraCandidateId;
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
    }

    public static CourseExtraVote create(Long courseExtraCandidateId, Long memberId) {
        return new CourseExtraVote(courseExtraCandidateId, memberId);
    }
}
