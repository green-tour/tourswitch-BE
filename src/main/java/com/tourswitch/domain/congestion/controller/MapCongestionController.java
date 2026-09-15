package com.tourswitch.domain.congestion.controller;

import com.tourswitch.domain.congestion.response.MapCongestionResponseDTO;
import com.tourswitch.domain.congestion.service.MapCongestionService;
import com.tourswitch.global.response.GlobalRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트 지도에 필요한 서울 영역별 혼잡도 데이터를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class MapCongestionController {

    private final MapCongestionService mapCongestionService;

    /**
     * 서울 121개 영역의 폴리곤과 최신 혼잡도 지도 데이터를 조회한다.
     */
    @GetMapping("/congestion")
    public GlobalRes<MapCongestionResponseDTO> getCongestionMap() {
        return GlobalRes.success(mapCongestionService.getCongestionMap());
    }
}
