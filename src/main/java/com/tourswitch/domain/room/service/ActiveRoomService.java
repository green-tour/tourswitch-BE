package com.tourswitch.domain.room.service;

import com.tourswitch.domain.room.repository.ActiveRoomQueryRepository;
import com.tourswitch.domain.room.response.ActiveRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActiveRoomService {
    private final ActiveRoomQueryRepository activeRoomQueryRepository;

    public ActiveRoomResponse getActiveRoom(Long memberId) {
        return activeRoomQueryRepository.findLatestByMemberId(memberId)
                .map(room -> ActiveRoomResponse.of(room, activeRoomQueryRepository.findKeywords(room.roomId())))
                .orElse(null);
    }
}
