package com.tourswitch.domain.data.response;

import com.tourswitch.domain.data.service.DerivedDataSyncResult;

public record DerivedDataSyncResponseDTO(
        int duplicateLinks,
        int keywordLinks,
        int crowdLinks,
        int crowdGradeRows,
        int crowdGradeThresholds,
        int areaLinks,
        int referencePopulationAreas
) {

    public static DerivedDataSyncResponseDTO from(DerivedDataSyncResult result) {
        return new DerivedDataSyncResponseDTO(
                result.duplicateLinks(),
                result.keywordLinks(),
                result.crowdLinks(),
                result.crowdGradeRows(),
                result.crowdGradeThresholds(),
                result.areaLinks(),
                result.referencePopulationAreas()
        );
    }
}
