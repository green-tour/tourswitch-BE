package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.course.entity.CourseExtraVote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseExtraVoteRepository extends JpaRepository<CourseExtraVote, Long> {

    Optional<CourseExtraVote> findByCourseExtraCandidateIdAndMemberId(Long courseExtraCandidateId, Long memberId);
}
