package com.tourswitch.domain.congestion.controller;

import com.tourswitch.domain.congestion.response.MapCongestionResponseDTO;
import com.tourswitch.domain.congestion.service.MapCongestionService;
import com.tourswitch.global.response.GlobalRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class MapCongestionController {

    private final MapCongestionService mapCongestionService;

    @GetMapping("/congestion")
    public GlobalRes<MapCongestionResponseDTO> getCongestionMap() {
        return GlobalRes.success(mapCongestionService.getCongestionMap());
    }
}
