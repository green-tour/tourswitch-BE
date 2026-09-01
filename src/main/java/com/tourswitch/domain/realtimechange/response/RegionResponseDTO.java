package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.repository.RegionRow;

public record RegionResponseDTO(Long id, String districtCode, String districtName) {

    public static RegionResponseDTO from(RegionRow row) {
        return new RegionResponseDTO(row.id(), row.districtCode(), row.districtName());
    }
}
