package com.tourswitch.domain.vote.repository;

import com.tourswitch.domain.vote.entity.RecommendationConditionCounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationConditionCounterRepository
        extends JpaRepository<RecommendationConditionCounter, String> {
}
