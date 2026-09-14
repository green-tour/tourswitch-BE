package com.tourswitch.domain.place.response;

public record PlaceAccessibilityResponseDTO(
        boolean wheelchairAccessible,
        boolean strollerAccessible,
        String wheelchairDescription,
        String strollerDescription
) {
}
