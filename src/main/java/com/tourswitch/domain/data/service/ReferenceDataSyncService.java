package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.RegionSeed;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReferenceDataSyncService {

    private static final List<RegionSeed> SEOUL_REGIONS = List.of(
            new RegionSeed("11110", "종로구"), new RegionSeed("11140", "중구"),
            new RegionSeed("11170", "용산구"), new RegionSeed("11200", "성동구"),
            new RegionSeed("11215", "광진구"), new RegionSeed("11230", "동대문구"),
            new RegionSeed("11260", "중랑구"), new RegionSeed("11290", "성북구"),
            new RegionSeed("11305", "강북구"), new RegionSeed("11320", "도봉구"),
            new RegionSeed("11350", "노원구"), new RegionSeed("11380", "은평구"),
            new RegionSeed("11410", "서대문구"), new RegionSeed("11440", "마포구"),
            new RegionSeed("11470", "양천구"), new RegionSeed("11500", "강서구"),
            new RegionSeed("11530", "구로구"), new RegionSeed("11545", "금천구"),
            new RegionSeed("11560", "영등포구"), new RegionSeed("11590", "동작구"),
            new RegionSeed("11620", "관악구"), new RegionSeed("11650", "서초구"),
            new RegionSeed("11680", "강남구"), new RegionSeed("11710", "송파구"),
            new RegionSeed("11740", "강동구")
    );

    private final ExternalDataSyncRepository repository;

    @Transactional
    public void synchronize() {
        repository.seedRegions(SEOUL_REGIONS);
    }
}
