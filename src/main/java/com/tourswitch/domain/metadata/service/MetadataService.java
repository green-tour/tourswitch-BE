package com.tourswitch.domain.metadata.service;

import com.tourswitch.domain.metadata.repository.MetadataQueryRepository;
import com.tourswitch.domain.metadata.response.KeywordMetadataResponse;
import com.tourswitch.domain.metadata.response.RegionMetadataResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetadataService {
    private final MetadataQueryRepository metadataQueryRepository;

    public List<RegionMetadataResponse> getRegions() {
        return metadataQueryRepository.findRegions().stream().map(RegionMetadataResponse::from).toList();
    }

    public List<KeywordMetadataResponse> getKeywords() {
        return metadataQueryRepository.findActiveKeywords().stream().map(KeywordMetadataResponse::from).toList();
    }
}
