package com.tourswitch.domain.room.repository;

import com.tourswitch.domain.room.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    boolean existsByTravelRoomIdAndMemberId(Long travelRoomId, Long memberId);
}
