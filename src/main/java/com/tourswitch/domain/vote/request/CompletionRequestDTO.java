package com.tourswitch.domain.vote.request;

import jakarta.validation.constraints.NotNull;

public record CompletionRequestDTO(
        @NotNull(message = "completed는 필수입니다.") Boolean completed
) {
}
