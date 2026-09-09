package com.tourswitch.domain.room.repository;

import com.tourswitch.domain.room.entity.TravelRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRoomRepository extends JpaRepository<TravelRoom, Long> {
    boolean existsByInviteToken(String inviteToken);
}
