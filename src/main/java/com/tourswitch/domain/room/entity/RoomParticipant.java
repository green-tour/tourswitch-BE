package com.tourswitch.domain.room.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "room_participant", uniqueConstraints = @UniqueConstraint(name = "uk_room_participant", columnNames = {"travel_room_id", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "travel_room_id", nullable = false)
    private Long travelRoomId;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "is_host", nullable = false)
    private boolean host;
    @Column(name = "is_selection_completed", nullable = false)
    private boolean selectionCompleted;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    public static RoomParticipant createHost(Long roomId, Long memberId) {
        RoomParticipant participant = new RoomParticipant();
        participant.travelRoomId = roomId;
        participant.memberId = memberId;
        participant.host = true;
        participant.selectionCompleted = false;
        participant.joinedAt = LocalDateTime.now();
        return participant;
    }
}
