package com.tourswitch.domain.metadata.response;

import com.tourswitch.domain.metadata.repository.RegionMetadataRow;

public record RegionMetadataResponse(Long id, String name) {
    public static RegionMetadataResponse from(RegionMetadataRow row) {
        return new RegionMetadataResponse(row.id(), row.name());
    }
}
