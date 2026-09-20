package com.tourswitch.domain.realtimechange.service;

import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.domain.place.service.DongCentroid;
import com.tourswitch.domain.place.service.PlaceCacheRefreshedEvent;
import com.tourswitch.domain.place.service.SeoulPlaceCache;
import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import com.tourswitch.domain.realtimechange.repository.AdministrativeDongRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 캐시에서 도출한 동을 기준정보 테이블에 반영한다.
 *
 * TourAPI가 동 단위 기준정보를 주지 않아 별도로 적재할 원천이 없다. 대신 이미 받아 둔 장소들의
 * 주소에서 동을 뽑아 채운다. 추가 API 호출이 생기지 않는 것이 이 방식을 고른 이유다.
 *
 * 테이블을 없애지 않고 채우는 쪽을 택했다. 동 id가 화면 요청과 course_replacement 이력에
 * 그대로 쓰이므로, 매번 새로 만들면 지난 교체 이력이 가리키는 대상이 사라진다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeDongSyncService {

    /** 좌표 컬럼이 소수점 7자리라 그 이상은 저장되지 않는다. */
    private static final int COORDINATE_SCALE = 7;
    /** 대표 좌표가 장소 한 곳으로만 정해지면 동 전체를 대표한다고 보기 어렵다. */
    private static final int MIN_PLACE_COUNT = 2;

    private final SeoulPlaceCache seoulPlaceCache;
    private final PlaceRegionQueryRepository placeRegionQueryRepository;
    private final AdministrativeDongRepository administrativeDongRepository;

    @EventListener(PlaceCacheRefreshedEvent.class)
    @Transactional
    public void syncFromCache() {
        int created = 0;
        int updated = 0;
        int deactivated = 0;

        for (PlaceRegionRow region : placeRegionQueryRepository.findAll()) {
            List<DongCentroid> centroids = seoulPlaceCache.findDongCentroids(region.districtName()).stream()
                    .filter(centroid -> centroid.placeCount() >= MIN_PLACE_COUNT)
                    .toList();
            if (centroids.isEmpty()) {
                // 캐시가 비어 있는 자치구까지 비활성으로 만들면, 갱신이 한 번 실패했을 때
                // 멀쩡하던 동 목록이 통째로 사라진다.
                continue;
            }

            // 비활성 행까지 봐야 한다. 활성만 보면 사라졌다 다시 나타난 동에서 새로 저장하려다
            // (region_id, dong_name) 고유 제약에 걸린다.
            Map<String, AdministrativeDong> existingByName = administrativeDongRepository
                    .findByRegionId(region.regionId()).stream()
                    .collect(Collectors.toMap(AdministrativeDong::getDongName, Function.identity(),
                            (left, right) -> left));
            Set<String> derivedNames = new HashSet<>();

            for (DongCentroid centroid : centroids) {
                derivedNames.add(centroid.dongName());
                AdministrativeDong existing = existingByName.get(centroid.dongName());
                if (existing == null) {
                    administrativeDongRepository.save(AdministrativeDong.create(
                            region.regionId(),
                            dongCodeOf(region.regionId(), centroid.dongName()),
                            centroid.dongName(),
                            scaled(centroid.centerLatitude()),
                            scaled(centroid.centerLongitude())));
                    created++;
                    continue;
                }
                boolean moved = existing.relocate(scaled(centroid.centerLatitude()),
                        scaled(centroid.centerLongitude()));
                boolean revived = Boolean.FALSE.equals(existing.getIsActive());
                if (revived) {
                    existing.activate();
                }
                if (moved || revived) {
                    updated++;
                }
            }

            for (AdministrativeDong stale : existingByName.values()) {
                if (!derivedNames.contains(stale.getDongName()) && Boolean.TRUE.equals(stale.getIsActive())) {
                    stale.deactivate();
                    deactivated++;
                }
            }
        }

        log.info("동 기준정보 동기화 완료. 신규={}건, 좌표갱신={}건, 비활성={}건", created, updated, deactivated);
    }

    private static BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * dong_code는 고유 제약이 걸린 필수 값인데 주소에서는 코드를 얻을 수 없다.
     * 자치구와 동 이름으로 결정적인 값을 만든다. 한글을 바이트로 잘라 쓰면 충돌하므로 해시를 쓴다.
     */
    private static String dongCodeOf(Long regionId, String dongName) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((regionId + ":" + dongName).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return "D" + regionId + "-" + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다.", e);
        }
    }
}
