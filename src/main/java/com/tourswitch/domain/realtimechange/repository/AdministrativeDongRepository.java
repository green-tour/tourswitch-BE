package com.tourswitch.domain.realtimechange.repository;

import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrativeDongRepository extends JpaRepository<AdministrativeDong, Long> {

    List<AdministrativeDong> findByRegionIdAndIsActiveTrueOrderByDongNameAsc(Long regionId);

    /** 동기화용. 비활성 행도 함께 봐야 다시 나타난 동에서 고유 제약에 걸리지 않는다. */
    List<AdministrativeDong> findByRegionId(Long regionId);

    Optional<AdministrativeDong> findByIdAndIsActiveTrue(Long id);
}
