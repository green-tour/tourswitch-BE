package com.tourswitch.domain.room.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record CreateTravelRoomRequest(
        @Size(max = 100, message = "방 이름은 100자 이하여야 합니다.") String roomName,
        @NotNull(message = "여행일은 필수입니다.") @FutureOrPresent(message = "여행일은 오늘 이후여야 합니다.") LocalDate travelDate,
        @NotNull(message = "자치구는 필수입니다.") @Positive(message = "자치구 ID가 올바르지 않습니다.") Long regionId,
        @NotNull(message = "키워드는 필수입니다.") @Size(min = 1, max = 4, message = "키워드는 1~4개를 선택해야 합니다.") List<@NotNull @Positive Long> keywordIds,
        @Min(value = 3, message = "코스 장소 수는 3개 이상이어야 합니다.") @Max(value = 6, message = "코스 장소 수는 6개 이하여야 합니다.") int courseSpotCount,
        boolean includesFood,
        boolean includesLodging,
        boolean includesShopping
) {
}
