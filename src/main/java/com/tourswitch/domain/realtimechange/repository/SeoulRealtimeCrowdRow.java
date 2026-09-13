package com.tourswitch.domain.realtimechange.repository;

import java.time.LocalDateTime;

public record SeoulRealtimeCrowdRow(String congestionLevel, LocalDateTime observedAt) {
}
