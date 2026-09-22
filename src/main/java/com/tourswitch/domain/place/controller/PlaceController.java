package com.tourswitch.domain.place.controller;

import com.tourswitch.domain.place.response.PlaceDetailResponseDTO;
import com.tourswitch.domain.place.response.PlaceFavoriteResponseDTO;
import com.tourswitch.domain.place.response.PlaceSummaryResponseDTO;
import com.tourswitch.domain.place.service.PlaceFavoriteService;
import com.tourswitch.domain.place.service.PlaceSearchService;
import com.tourswitch.global.response.GlobalRes;
import com.tourswitch.global.response.PageRes;
import com.tourswitch.global.security.principal.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관광지 검색과 상세 조회 API를 제공한다.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceSearchService placeSearchService;
    private final PlaceFavoriteService placeFavoriteService;

    /**
     * 조건에 맞는 관광지 목록을 페이지 단위로 조회한다.
     */
    @GetMapping
    public GlobalRes<PageRes<PlaceSummaryResponseDTO>> search(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) List<String> keywordCodes,
            @RequestParam(required = false) String congestionLevel,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return GlobalRes.success(placeSearchService.search(regionId, keywordCodes, congestionLevel, page, size));
    }

    /**
     * 관광지 기본 정보와 최대 7일의 혼잡도 예측을 조회한다.
     */
    @GetMapping("/{contentId}")
    public GlobalRes<PlaceDetailResponseDTO> getDetail(@PathVariable String contentId,
                                                        @RequestParam(required = false) Long regionId) {
        return GlobalRes.success(placeSearchService.getDetail(contentId, regionId));
    }

    /**
     * 현재 회원의 관광지 찜 여부를 조회한다.
     */
    @GetMapping("/{contentId}/favorite")
    public GlobalRes<PlaceFavoriteResponseDTO> getFavorite(
            @PathVariable String contentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(placeFavoriteService.getFavorite(principal.memberId(), contentId));
    }

    /**
     * 현재 회원의 찜 목록에 관광지를 추가한다.
     */
    @PostMapping("/{contentId}/favorite")
    public GlobalRes<PlaceFavoriteResponseDTO> addFavorite(
            @PathVariable String contentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(placeFavoriteService.addFavorite(principal.memberId(), contentId));
    }

    /**
     * 현재 회원의 찜 목록에서 관광지를 제거한다.
     */
    @DeleteMapping("/{contentId}/favorite")
    public GlobalRes<PlaceFavoriteResponseDTO> removeFavorite(
            @PathVariable String contentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(placeFavoriteService.removeFavorite(principal.memberId(), contentId));
    }
}
