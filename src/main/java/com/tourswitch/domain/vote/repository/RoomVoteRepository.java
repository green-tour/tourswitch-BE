package com.tourswitch.domain.vote.repository;

import com.tourswitch.domain.vote.entity.RoomVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomVoteRepository extends JpaRepository<RoomVote, Long> {

    Optional<RoomVote> findByRoomCandidateIdAndMemberId(Long roomCandidateId, Long memberId);

    List<RoomVote> findByRoomCandidateIdIn(List<Long> roomCandidateIds);

    long countByRoomCandidateId(Long roomCandidateId);
}
