package com.tourswitch.domain.place.provider;

import com.tourswitch.domain.place.model.FavoritePlace;
import com.tourswitch.global.client.tourapi.KorServiceClient;
import com.tourswitch.global.client.tourapi.TourApiSpotDetail;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 찜한 TourAPI contentId로 관광지의 최신 표시 정보를 제공한다.
 */
@Component
@RequiredArgsConstructor
public class FavoritePlaceProvider {

    private final KorServiceClient korServiceClient;

    /**
     * TourAPI 상세 조회 결과를 찜 목록용 모델로 변환한다.
     */
    public Optional<FavoritePlace> findByContentId(String contentId) {
        return korServiceClient.detailCommon2(contentId).map(this::toFavoritePlace);
    }

    /**
     * TourAPI 상세 응답에서 화면에 필요한 최신 필드만 선택한다.
     */
    private FavoritePlace toFavoritePlace(TourApiSpotDetail detail) {
        return new FavoritePlace(detail.contentId(), detail.title(), detail.firstImageUrl(), detail.address());
    }
}
