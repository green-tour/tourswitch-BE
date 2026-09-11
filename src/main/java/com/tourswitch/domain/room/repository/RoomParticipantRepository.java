package com.tourswitch.domain.room.repository;

import com.tourswitch.domain.room.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    boolean existsByTravelRoomIdAndMemberId(Long travelRoomId, Long memberId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO room_participant
                (travel_room_id, member_id, is_host, is_selection_completed, joined_at)
            VALUES (:roomId, :memberId, FALSE, FALSE, NOW())
            """, nativeQuery = true)
    int insertGuestIfAbsent(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
