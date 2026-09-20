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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부가 카테고리(음식/숙박/쇼핑) 후보 스냅샷.
 * 관광지 투표가 끝난 뒤 확정 경유지 기준으로 만들어지고, 추가 투표 라운드에서 참여자가 고른다.
 * title/imageUrl은 카드에 그대로 노출하며 설명은 고를 때 상세 조회로 채운다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_extra_candidate", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_extra_candidate",
                columnNames = {"course_id", "anchor_course_spot_id", "content_id"})
})
public class CourseExtraCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anchor_course_spot_id", nullable = false)
    private CourseSpot anchorCourseSpot;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    @Column(name = "title_snapshot", length = 200)
    private String titleSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    private String imageUrlSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_role", nullable = false, length = 20)
    private SpotRole spotRole;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected;

    private CourseExtraCandidate(Course course, CourseSpot anchorCourseSpot, String contentId, String titleSnapshot,
                                  String imageUrlSnapshot, SpotRole spotRole, Integer distanceMeters,
                                  Integer displayOrder) {
        this.course = course;
        this.anchorCourseSpot = anchorCourseSpot;
        this.contentId = contentId;
        this.titleSnapshot = titleSnapshot;
        this.imageUrlSnapshot = imageUrlSnapshot;
        this.spotRole = spotRole;
        this.distanceMeters = distanceMeters;
        this.displayOrder = displayOrder;
        this.isSelected = false;
    }

    public static CourseExtraCandidate create(Course course, CourseSpot anchorCourseSpot, String contentId,
                                               String titleSnapshot, String imageUrlSnapshot, SpotRole spotRole,
                                               Integer distanceMeters, Integer displayOrder) {
        return new CourseExtraCandidate(course, anchorCourseSpot, contentId, titleSnapshot, imageUrlSnapshot,
                spotRole, distanceMeters, displayOrder);
    }

    public void select() {
        this.isSelected = true;
    }
}
