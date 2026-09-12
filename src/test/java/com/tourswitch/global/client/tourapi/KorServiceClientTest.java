package com.tourswitch.global.client.tourapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 TourAPI를 호출해 클라이언트의 응답 파싱이 맞는지 확인한다(계획 문서 10절 1단계).
 * 대부분의 테스트와 달리 외부 API를 목킹하지 않는다 - 응답 스키마 자체를 검증하는 게 목적이다.
 * TOUR_API_KEY_KORSERVICE/TOUR_API_KEY 환경변수가 필요하다.
 */
@SpringBootTest
class KorServiceClientTest {

    @Autowired
    private KorServiceClient korServiceClient;

    @Autowired
    private TatsCnctrRateClient tatsCnctrRateClient;

    @Test
    void 지역과_콘텐츠타입으로_관광지_목록을_받아온다() {
        List<TourApiSpotItem> spots = korServiceClient.areaBasedList2("11", "140", 12, null);

        assertThat(spots).isNotEmpty();
        TourApiSpotItem first = spots.get(0);
        assertThat(first.contentId()).isNotBlank();
        assertThat(first.title()).isNotBlank();
        assertThat(first.latitude()).isBetween(33.0, 39.0);
        assertThat(first.longitude()).isBetween(124.0, 132.0);
    }

    @Test
    void 좌표와_반경으로_근처_음식점을_받아온다() {
        List<TourApiSpotItem> spots = korServiceClient.locationBasedList2(37.5569426391, 126.9840878410, 1000, 39);

        assertThat(spots).isNotEmpty();
        assertThat(spots.get(0).distanceMeters()).isNotNull();
    }

    @Test
    void 콘텐츠ID로_상세정보를_받아온다() {
        Optional<TourApiSpotDetail> detail = korServiceClient.detailCommon2("128776");

        assertThat(detail).isPresent();
        assertThat(detail.get().title()).isNotBlank();
    }

    @Test
    void 지역_집중률_목록을_받아온다() {
        List<TourApiCongestionItem> items = tatsCnctrRateClient.tatsCnctrRatedList("11", "11140");

        assertThat(items).isNotEmpty();
        assertThat(items.get(0).touristSpotName()).isNotBlank();
    }
}
