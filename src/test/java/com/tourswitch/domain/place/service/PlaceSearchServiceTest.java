package com.tourswitch.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.place.exception.PlaceNotFoundException;
import com.tourswitch.domain.place.exception.RegionNotFoundException;
import com.tourswitch.domain.place.repository.PlaceKeywordClassificationQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionQueryRepository;
import com.tourswitch.domain.place.repository.PlaceRegionRow;
import com.tourswitch.domain.place.response.PlaceSummaryResponseDTO;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.client.tourapi.TourApiSpotDetail;
import com.tourswitch.global.client.tourapi.TourApiSpotItem;
import com.tourswitch.global.response.PageRes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {
    @Mock KorServiceClient korServiceClient;
    @Mock TatsCnctrRateClient tatsCnctrRateClient;
    @Mock PlaceRegionQueryRepository placeRegionQueryRepository;
    @Mock PlaceKeywordClassificationQueryRepository placeKeywordClassificationQueryRepository;

    PlaceSearchService service;

    @BeforeEach
    void setUp() {
        service = new PlaceSearchService(korServiceClient, tatsCnctrRateClient, placeRegionQueryRepository,
                placeKeywordClassificationQueryRepository);
    }

    private TourApiSpotItem spot(String contentId, String title) {
        return new TourApiSpotItem(contentId, 12, title, "주소", null, 37.5, 127.0, "A01", "A0101", "A010101", null);
    }

    @Test
    void 지역_없이_검색하면_서울_전체를_district_없이_조회하고_혼잡도는_비운다() {
        when(korServiceClient.areaBasedList2(eq("11"), isNull(), eq(12), isNull()))
                .thenReturn(List.of(spot("1", "서울숲")));

        PageRes<PlaceSummaryResponseDTO> result = service.search(null, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).regionName()).isNull();
        assertThat(result.items().get(0).congestion().level()).isNull();
        verify(tatsCnctrRateClient, never()).tatsCnctrRatedList(any(), any());
    }

    @Test
    void 지역을_지정하면_해당_지역_코드로_조회하고_이름이_맞는_혼잡도_등급을_채운다() {
        PlaceRegionRow region = new PlaceRegionRow("성동구", "11", "260", "11200");
        when(placeRegionQueryRepository.findById(1L)).thenReturn(Optional.of(region));
        when(korServiceClient.areaBasedList2(eq("11"), eq("260"), eq(12), isNull()))
                .thenReturn(List.of(spot("1", "서울숲")));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        when(tatsCnctrRateClient.tatsCnctrRatedList("11", "11200"))
                .thenReturn(List.of(new TourApiCongestionItem("서울숲", today, new BigDecimal("10"))));

        PageRes<PlaceSummaryResponseDTO> result = service.search(1L, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).regionName()).isEqualTo("성동구");
        assertThat(result.items().get(0).congestion().level()).isEqualTo("여유");
    }

    @Test
    void FE_카테고리_코드는_해당_키워드명의_분류코드로_변환돼_조회된다() {
        when(placeKeywordClassificationQueryRepository.findClassificationLevel2CodesByKeywordName("역사유적"))
                .thenReturn(List.of("A0201"));
        when(korServiceClient.areaBasedList2(eq("11"), isNull(), eq(12), eq("A0201")))
                .thenReturn(List.of(spot("1", "경복궁")));

        PageRes<PlaceSummaryResponseDTO> result = service.search(null, List.of("HISTORICAL_RELIC"), 1, 20);

        assertThat(result.items()).hasSize(1);
        verify(placeKeywordClassificationQueryRepository, never()).findClassificationLevel2CodesByKeywordId(any());
    }

    @Test
    void 알_수_없는_카테고리_코드는_결과가_비어있다() {
        PageRes<PlaceSummaryResponseDTO> result = service.search(null, List.of("UNKNOWN_CODE"), 1, 20);

        assertThat(result.items()).isEmpty();
        verify(korServiceClient, never()).areaBasedList2(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void 존재하지_않는_지역이면_예외() {
        when(placeRegionQueryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.search(999L, null, 1, 20)).isInstanceOf(RegionNotFoundException.class);
    }

    @Test
    void 같은_장소가_여러_컨텐츠타입으로_중복_조회돼도_한_번만_남는다() {
        lenient().when(korServiceClient.areaBasedList2(eq("11"), isNull(), eq(12), isNull()))
                .thenReturn(List.of(spot("1", "서울숲")));
        lenient().when(korServiceClient.areaBasedList2(eq("11"), isNull(), eq(14), isNull()))
                .thenReturn(List.of(spot("1", "서울숲")));

        PageRes<PlaceSummaryResponseDTO> result = service.search(null, null, 1, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void 페이지네이션이_올바르게_동작한다() {
        when(korServiceClient.areaBasedList2(eq("11"), isNull(), eq(12), isNull()))
                .thenReturn(List.of(spot("1", "a"), spot("2", "b"), spot("3", "c")));

        PageRes<PlaceSummaryResponseDTO> firstPage = service.search(null, null, 1, 2);
        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();

        PageRes<PlaceSummaryResponseDTO> secondPage = service.search(null, null, 2, 2);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 존재하지_않는_콘텐츠ID면_상세조회_예외() {
        when(korServiceClient.detailCommon2("999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDetail("999", null)).isInstanceOf(PlaceNotFoundException.class);
    }

    @Test
    void 상세조회는_지역이_있으면_혼잡도를_함께_반환한다() {
        TourApiSpotDetail detail = new TourApiSpotDetail("1", "서울숲", "설명", "주소", "img", 37.5, 127.0);
        when(korServiceClient.detailCommon2("1")).thenReturn(Optional.of(detail));
        PlaceRegionRow region = new PlaceRegionRow("성동구", "11", "260", "11200");
        when(placeRegionQueryRepository.findById(1L)).thenReturn(Optional.of(region));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        when(tatsCnctrRateClient.tatsCnctrRatedList("11", "11200"))
                .thenReturn(List.of(new TourApiCongestionItem("서울숲", today, new BigDecimal("60"))));

        var response = service.getDetail("1", 1L);

        assertThat(response.regionName()).isEqualTo("성동구");
        assertThat(response.congestion().level()).isEqualTo("약간 붐빔");
        assertThat(response.congestion().rate()).isEqualByComparingTo("60");
    }
}
