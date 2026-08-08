package com.tourswitch.global.response;

import java.util.List;

public record PageRes<T>(List<T> items, long totalCount, int page, int size, boolean hasNext) {
}
