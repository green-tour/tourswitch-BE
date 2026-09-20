package com.tourswitch.domain.realtimechange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.domain.place.service.DongCentroid;
import com.tourswitch.domain.place.service.SeoulPlaceCache;
import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import com.tourswitch.domain.realtimechange.repository.AdministrativeDongRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 동 기준정보는 캐시에서 파생되지만 id가 교체 이력에 쓰인다. 갱신할 때 행을 새로 만들지 않고
 * 기존 행을 이어 쓰는지, 캐시가 비었을 때 멀쩡한 목록을 지우지 않는지를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class AdministrativeDongSyncServiceTest {

    private static final Long REGION_ID = 1L;
    private static final String DISTRICT = "종로구";

    @Mock SeoulPlaceCache seoulPlaceCache;
    @Mock PlaceRegionQueryRepository placeRegionQueryRepository;
    @Mock AdministrativeDongRepository administrativeDongRepository;

    AdministrativeDongSyncService service;

    @BeforeEach
    void setUp() {
        service = new AdministrativeDongSyncService(
                seoulPlaceCache, placeRegionQueryRepository, administrativeDongRepository);
        given(placeRegionQueryRepository.findAll())
                .willReturn(List.of(new PlaceRegionRow(REGION_ID, DISTRICT, "11", "110", "11110")));
    }

    private static DongCentroid centroid(String name, double lat, double lng, int placeCount) {
        return new DongCentroid(name, lat, lng, placeCount);
    }

    private static AdministrativeDong dong(String name, double lat, double lng) {
        return AdministrativeDong.create(REGION_ID, "D1-000000000000", name,
                BigDecimal.valueOf(lat).setScale(7, java.math.RoundingMode.HALF_UP),
                BigDecimal.valueOf(lng).setScale(7, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void 새로_도출된_동을_저장한다() {
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("가회동", 37.5817, 126.9849, 7)));
        given(administrativeDongRepository.findByRegionId(REGION_ID)).willReturn(List.of());

        service.syncFromCache();

        ArgumentCaptor<AdministrativeDong> saved = ArgumentCaptor.forClass(AdministrativeDong.class);
        verify(administrativeDongRepository).save(saved.capture());
        assertThat(saved.getValue().getDongName()).isEqualTo("가회동");
        assertThat(saved.getValue().getRegionId()).isEqualTo(REGION_ID);
    }

    @Test
    void 장소가_한_곳뿐인_동은_대표성이_없어_제외한다() {
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("혼자동", 37.58, 126.98, 1)));

        service.syncFromCache();

        verify(administrativeDongRepository, never()).save(any());
        verify(administrativeDongRepository, never()).findByRegionId(anyLong());
    }

    @Test
    void 이미_있는_동은_새로_만들지_않고_좌표만_옮긴다() {
        AdministrativeDong existing = dong("가회동", 37.5000000, 126.9000000);
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("가회동", 37.5817, 126.9849, 7)));
        given(administrativeDongRepository.findByRegionId(REGION_ID)).willReturn(List.of(existing));

        service.syncFromCache();

        verify(administrativeDongRepository, never()).save(any());
        assertThat(existing.getCenterLatitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5817000));
        assertThat(existing.getIsActive()).isTrue();
    }

    @Test
    void 사라진_동은_지우지_않고_비활성으로_둔다() {
        AdministrativeDong staying = dong("가회동", 37.5817, 126.9849);
        AdministrativeDong gone = dong("없어진동", 37.6, 127.0);
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("가회동", 37.5817, 126.9849, 7)));
        given(administrativeDongRepository.findByRegionId(REGION_ID)).willReturn(List.of(staying, gone));

        service.syncFromCache();

        assertThat(gone.getIsActive()).isFalse();
        assertThat(staying.getIsActive()).isTrue();
    }

    @Test
    void 비활성이던_동이_다시_나오면_되살린다() {
        AdministrativeDong revived = dong("가회동", 37.5817, 126.9849);
        revived.deactivate();
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("가회동", 37.5817, 126.9849, 7)));
        given(administrativeDongRepository.findByRegionId(REGION_ID)).willReturn(List.of(revived));

        service.syncFromCache();

        // 새로 저장하면 (region_id, dong_name) 고유 제약에 걸린다.
        verify(administrativeDongRepository, never()).save(any());
        assertThat(revived.getIsActive()).isTrue();
    }

    @Test
    void 캐시가_빈_자치구는_기존_목록을_건드리지_않는다() {
        given(seoulPlaceCache.findDongCentroids(DISTRICT)).willReturn(List.of());

        service.syncFromCache();

        verify(administrativeDongRepository, never()).findByRegionId(anyLong());
        verify(administrativeDongRepository, never()).save(any());
    }

    @Test
    void 자치구와_동_이름이_같으면_같은_코드를_만든다() {
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("가회동", 37.5817, 126.9849, 7)));
        given(administrativeDongRepository.findByRegionId(REGION_ID)).willReturn(List.of());

        service.syncFromCache();
        ArgumentCaptor<AdministrativeDong> first = ArgumentCaptor.forClass(AdministrativeDong.class);
        verify(administrativeDongRepository).save(first.capture());

        assertThat(first.getValue().getDongCode())
                .startsWith("D" + REGION_ID + "-")
                .hasSizeLessThanOrEqualTo(20);
    }

    @Test
    void 동_이름이_다르면_코드도_다르다() {
        given(seoulPlaceCache.findDongCentroids(DISTRICT))
                .willReturn(List.of(centroid("가회동", 37.58, 126.98, 3), centroid("삼청동", 37.59, 126.98, 4)));
        given(administrativeDongRepository.findByRegionId(REGION_ID)).willReturn(List.of());

        service.syncFromCache();

        ArgumentCaptor<AdministrativeDong> saved = ArgumentCaptor.forClass(AdministrativeDong.class);
        verify(administrativeDongRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(AdministrativeDong::getDongCode).doesNotHaveDuplicates();
    }
}
