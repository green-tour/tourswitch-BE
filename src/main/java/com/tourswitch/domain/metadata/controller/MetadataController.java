package com.tourswitch.domain.metadata.controller;

import com.tourswitch.domain.metadata.response.KeywordMetadataResponse;
import com.tourswitch.domain.metadata.response.RegionMetadataResponse;
import com.tourswitch.domain.metadata.service.MetadataService;
import com.tourswitch.global.response.GlobalRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metadata")
public class MetadataController {
    private final MetadataService metadataService;

    @GetMapping("/regions")
    public GlobalRes<List<RegionMetadataResponse>> getRegions() {
        return GlobalRes.success(metadataService.getRegions());
    }

    @GetMapping("/keywords")
    public GlobalRes<List<KeywordMetadataResponse>> getKeywords() {
        return GlobalRes.success(metadataService.getKeywords());
    }
}
