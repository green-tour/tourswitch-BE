package com.tourswitch.domain.place.controller;

import com.tourswitch.domain.place.response.PlaceDetailResponseDTO;
import com.tourswitch.domain.place.response.PlaceSummaryResponseDTO;
import com.tourswitch.domain.place.service.PlaceSearchService;
import com.tourswitch.global.response.GlobalRes;
import com.tourswitch.global.response.PageRes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceSearchService placeSearchService;

    @GetMapping
    public GlobalRes<PageRes<PlaceSummaryResponseDTO>> search(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) List<String> keywordCodes,
            @RequestParam(required = false) String congestionLevel,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return GlobalRes.success(placeSearchService.search(regionId, keywordCodes, congestionLevel, page, size));
    }

    @GetMapping("/{contentId}")
    public GlobalRes<PlaceDetailResponseDTO> getDetail(@PathVariable String contentId,
                                                        @RequestParam(required = false) Long regionId) {
        return GlobalRes.success(placeSearchService.getDetail(contentId, regionId));
    }
}
