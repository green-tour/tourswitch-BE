package com.tourswitch.domain.realtimechange.repository;

import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrativeDongRepository extends JpaRepository<AdministrativeDong, Long> {

    List<AdministrativeDong> findByRegionIdAndIsActiveTrueOrderByDongNameAsc(Long regionId);

    Optional<AdministrativeDong> findByIdAndIsActiveTrue(Long id);
}
