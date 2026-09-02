package com.tourswitch.domain.room.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "travel_room", uniqueConstraints = @UniqueConstraint(name = "uk_travel_room_invite_token", columnNames = "invite_token"))
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "invite_token", nullable = false, length = 64)
    private String inviteToken;
    @Column(name = "host_member_id", nullable = false)
    private Long hostMemberId;
    @Column(name = "room_name")
    private String roomName;
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;
    @Column(name = "region_id", nullable = false)
    private Long regionId;
    @Column(name = "course_spot_count", nullable = false)
    private Integer courseSpotCount;
    @Column(name = "includes_food", nullable = false)
    private boolean includesFood;
    @Column(name = "includes_lodging", nullable = false)
    private boolean includesLodging;
    @Column(name = "includes_shopping", nullable = false)
    private boolean includesShopping;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TravelRoomStatus status;
    @Column(name = "recommendation_condition_key", nullable = false, length = 64)
    private String recommendationConditionKey;
    @Column(name = "candidate_offset", nullable = false)
    private Integer candidateOffset;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static TravelRoom create(String inviteToken, Long hostMemberId, String roomName, LocalDate travelDate,
                                    Long regionId, int courseSpotCount, boolean includesFood,
                                    boolean includesLodging, boolean includesShopping,
                                    String conditionKey, int candidateOffset) {
        TravelRoom room = new TravelRoom();
        room.inviteToken = inviteToken;
        room.hostMemberId = hostMemberId;
        room.roomName = roomName;
        room.travelDate = travelDate;
        room.regionId = regionId;
        room.courseSpotCount = courseSpotCount;
        room.includesFood = includesFood;
        room.includesLodging = includesLodging;
        room.includesShopping = includesShopping;
        room.status = TravelRoomStatus.VOTING;
        room.recommendationConditionKey = conditionKey;
        room.candidateOffset = candidateOffset;
        return room;
    }
}
