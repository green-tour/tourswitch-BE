package com.tourswitch.domain.data.service;

public record DerivedDataSyncResult(
        int duplicateLinks,
        int keywordLinks,
        int crowdLinks,
        int crowdGradeRows,
        int crowdGradeThresholds,
        int areaLinks,
        int referencePopulationAreas
) {
}
