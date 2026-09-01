package com.tourswitch.domain.realtimechange.response;

import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;

public record AdministrativeDongResponseDTO(
        Long id,
        Long regionId,
        String dongCode,
        String dongName
) {

    public static AdministrativeDongResponseDTO from(AdministrativeDong dong) {
        return new AdministrativeDongResponseDTO(
                dong.getId(),
                dong.getRegionId(),
                dong.getDongCode(),
                dong.getDongName());
    }
}
