package com.tourswitch.domain.course.repository;

import com.tourswitch.domain.course.entity.SpotDailyDemand;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotDailyDemandRepository extends JpaRepository<SpotDailyDemand, Long> {

    Optional<SpotDailyDemand> findByTouristSpotIdAndTargetDate(Long touristSpotId, LocalDate targetDate);
}
