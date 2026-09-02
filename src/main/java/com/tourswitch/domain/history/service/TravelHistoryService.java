package com.tourswitch.domain.history.service;

import com.tourswitch.domain.history.repository.TravelHistoryQueryRepository;
import com.tourswitch.domain.history.response.TravelHistoryItemResponse;
import com.tourswitch.global.response.PageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelHistoryService {
    private final TravelHistoryQueryRepository queryRepository;

    public PageRes<TravelHistoryItemResponse> getHistories(Long memberId, int page, int size) {
        long totalCount = queryRepository.countHistories(memberId);
        var items = queryRepository.findHistories(memberId, page, size).stream()
                .map(TravelHistoryItemResponse::from)
                .toList();
        return new PageRes<>(items, totalCount, page, size, (long) (page + 1) * size < totalCount);
    }
}
