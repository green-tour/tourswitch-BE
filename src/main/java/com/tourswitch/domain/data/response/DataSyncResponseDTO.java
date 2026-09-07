package com.tourswitch.domain.data.response;

public record DataSyncResponseDTO(
        String source,
        int synced
) {

    public static DataSyncResponseDTO of(String source, int synced) {
        return new DataSyncResponseDTO(source, synced);
    }
}
