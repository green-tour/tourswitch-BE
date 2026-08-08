package com.tourswitch.domain.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * 카드 선택 = 한 표. 취소는 행 삭제로 처리한다(계획 문서 4단계).
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "room_vote", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_vote", columnNames = {"room_candidate_id", "member_id"})
})
public class RoomVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_candidate_id", nullable = false)
    private RoomCandidate roomCandidate;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @CreatedDate
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;

    private RoomVote(RoomCandidate roomCandidate, Long memberId) {
        this.roomCandidate = roomCandidate;
        this.memberId = memberId;
    }

    public static RoomVote create(RoomCandidate roomCandidate, Long memberId) {
        return new RoomVote(roomCandidate, memberId);
    }
}
