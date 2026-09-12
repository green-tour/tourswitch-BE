package com.tourswitch.domain.metadata.response;

import com.tourswitch.domain.metadata.model.KeywordCode;
import com.tourswitch.domain.metadata.repository.KeywordMetadataRow;

public record KeywordMetadataResponse(Long id, String name, String code) {
    public static KeywordMetadataResponse from(KeywordMetadataRow row) {
        return new KeywordMetadataResponse(row.id(), row.name(), KeywordCode.fromKeywordName(row.name()));
    }
}
