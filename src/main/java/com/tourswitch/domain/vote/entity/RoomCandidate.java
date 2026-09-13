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
 * contentId는 로컬 tourist_spot PK가 아니라 TourAPI contentId를 그대로 저장한다(관광지 마스터
 * 데이터를 로컬에 캐싱하지 않는다 - TourAPI 실시간전환 계획 문서 4절). titleSnapshot/좌표는
 * 코스생성(거리 계산, 부가후보 반경검색)이 재조회 없이 쓸 수 있도록 후보구성 시점 값을 그대로 담는다.
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "room_candidate", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_candidate", columnNames = {"travel_room_id", "content_id"})
})
public class RoomCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "travel_room_id", nullable = false)
    private Long travelRoomId;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

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

    @Column(name = "title_snapshot", nullable = false, length = 200)
    private String titleSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    private String imageUrlSnapshot;

    @Column(name = "latitude_snapshot", nullable = false)
    private Double latitudeSnapshot;

    @Column(name = "longitude_snapshot", nullable = false)
    private Double longitudeSnapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RoomCandidate(Long travelRoomId, String contentId, Long keywordId, Integer displayOrder,
                           BigDecimal recommendationScore, BigDecimal concentrationRateSnapshot,
                           String concentrationGradeSnapshot, String titleSnapshot, String imageUrlSnapshot,
                           Double latitudeSnapshot, Double longitudeSnapshot) {
        this.travelRoomId = travelRoomId;
        this.contentId = contentId;
        this.keywordId = keywordId;
        this.displayOrder = displayOrder;
        this.recommendationScore = recommendationScore;
        this.concentrationRateSnapshot = concentrationRateSnapshot;
        this.concentrationGradeSnapshot = concentrationGradeSnapshot;
        this.titleSnapshot = titleSnapshot;
        this.imageUrlSnapshot = imageUrlSnapshot;
        this.latitudeSnapshot = latitudeSnapshot;
        this.longitudeSnapshot = longitudeSnapshot;
    }

    public static RoomCandidate create(Long travelRoomId, String contentId, Long keywordId, Integer displayOrder,
                                        BigDecimal recommendationScore, BigDecimal concentrationRateSnapshot,
                                        String concentrationGradeSnapshot, String titleSnapshot,
                                        String imageUrlSnapshot, Double latitudeSnapshot, Double longitudeSnapshot) {
        return new RoomCandidate(travelRoomId, contentId, keywordId, displayOrder, recommendationScore,
                concentrationRateSnapshot, concentrationGradeSnapshot, titleSnapshot, imageUrlSnapshot,
                latitudeSnapshot, longitudeSnapshot);
    }
}
