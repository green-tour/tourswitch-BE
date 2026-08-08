package com.tourswitch.domain.vote.repository;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomCandidateRepository extends JpaRepository<RoomCandidate, Long> {

    List<RoomCandidate> findByTravelRoomIdOrderByDisplayOrderAsc(Long travelRoomId);

    boolean existsByTravelRoomId(Long travelRoomId);
}
