package com.tourswitch.domain.data.service;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.InvalidAreaBoundary;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.RegionSeed;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public ReferenceDataSyncResult synchronize() {
        repository.seedRegions(SEOUL_REGIONS);
        repository.seedRealtimeAreasWhenMissing();
        int repairedBoundaries = repairInvalidBoundaries();
        int realtimeAreaCount = repository.countRealtimeAreas();
        if (realtimeAreaCount != 121) {
            throw new IllegalStateException("서울 실시간 영역 기준정보가 121건이 아닙니다: " + realtimeAreaCount);
        }
        return new ReferenceDataSyncResult(SEOUL_REGIONS.size(), realtimeAreaCount, repairedBoundaries);
    }

    private int repairInvalidBoundaries() {
        List<InvalidAreaBoundary> invalidBoundaries = repository.findInvalidAreaBoundaries();
        for (InvalidAreaBoundary boundary : invalidBoundaries) {
            Polygon fixedPolygon = fixPolygon(boundary);
            repository.updateAreaBoundary(boundary.areaId(), new WKTWriter().write(fixedPolygon));
            log.warn("유효하지 않은 서울 영역 경계를 자동 복구했습니다: areaName={}", boundary.areaName());
        }
        return invalidBoundaries.size();
    }

    private Polygon fixPolygon(InvalidAreaBoundary boundary) {
        try {
            Geometry fixed = GeometryFixer.fix(new WKTReader().read(boundary.boundaryWkt()));
            if (fixed instanceof Polygon polygon) {
                return polygon;
            }
            if (fixed instanceof MultiPolygon multiPolygon && multiPolygon.getNumGeometries() > 0) {
                Polygon largest = null;
                for (int index = 0; index < multiPolygon.getNumGeometries(); index++) {
                    Polygon candidate = (Polygon) multiPolygon.getGeometryN(index);
                    if (largest == null || candidate.getArea() > largest.getArea()) {
                        largest = candidate;
                    }
                }
                return largest;
            }
            throw new IllegalStateException("영역 경계를 Polygon으로 복구할 수 없습니다: " + boundary.areaName());
        } catch (ParseException exception) {
            throw new IllegalStateException("영역 경계 WKT를 파싱할 수 없습니다: " + boundary.areaName(), exception);
        }
    }

    public record ReferenceDataSyncResult(int regions, int realtimeAreas, int repairedBoundaries) {
    }
}
