package com.tourswitch.domain.vote.repository;

import com.tourswitch.domain.vote.entity.RoomVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomVoteRepository extends JpaRepository<RoomVote, Long> {

    Optional<RoomVote> findByRoomCandidateIdAndMemberId(Long roomCandidateId, Long memberId);

    List<RoomVote> findByRoomCandidateIdIn(List<Long> roomCandidateIds);

    long countByRoomCandidateId(Long roomCandidateId);

    @Query("SELECT rv.roomCandidate.id AS candidateId, COUNT(rv) AS voteCount "
            + "FROM RoomVote rv WHERE rv.roomCandidate.id IN :candidateIds GROUP BY rv.roomCandidate.id")
    List<CandidateVoteCount> countGroupedByCandidateIds(@Param("candidateIds") List<Long> candidateIds);
}
