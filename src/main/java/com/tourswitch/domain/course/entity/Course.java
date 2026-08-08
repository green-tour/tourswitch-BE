package com.tourswitch.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 방 하나에 코스 하나(travel_room_id unique).
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "travel_room_id", nullable = false, unique = true)
    private Long travelRoomId;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "total_distance_meters")
    private Integer totalDistanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Course(Long travelRoomId, LocalDate travelDate) {
        this.travelRoomId = travelRoomId;
        this.travelDate = travelDate;
        this.status = CourseStatus.DRAFT;
    }

    public static Course create(Long travelRoomId, LocalDate travelDate) {
        return new Course(travelRoomId, travelDate);
    }

    public void assignTotalDistance(int totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public void confirm() {
        this.status = CourseStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
}
