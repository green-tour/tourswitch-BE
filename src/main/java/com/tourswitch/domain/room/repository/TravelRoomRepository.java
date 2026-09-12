package com.tourswitch.domain.room.repository;

import com.tourswitch.domain.room.entity.TravelRoom;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelRoomRepository extends JpaRepository<TravelRoom, Long> {
    boolean existsByInviteToken(String inviteToken);

    Optional<TravelRoom> findByInviteToken(String inviteToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT room FROM TravelRoom room WHERE room.inviteToken = :inviteToken")
    Optional<TravelRoom> findByInviteTokenForUpdate(@Param("inviteToken") String inviteToken);
}
