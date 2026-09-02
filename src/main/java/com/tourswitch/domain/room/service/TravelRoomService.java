package com.tourswitch.domain.room.service;

import com.tourswitch.domain.room.entity.*;
import com.tourswitch.domain.room.exception.InvalidTravelRoomRequestException;
import com.tourswitch.domain.room.repository.*;
import com.tourswitch.domain.room.request.CreateTravelRoomRequest;
import com.tourswitch.domain.room.response.CreateTravelRoomResponse;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.service.CandidateCompositionPlan;
import com.tourswitch.domain.vote.service.CandidateCompositionService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TravelRoomService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final TravelRoomRepository travelRoomRepository;
    private final RoomKeywordRepository roomKeywordRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomReferenceQueryRepository referenceQueryRepository;
    private final CandidateCompositionService candidateCompositionService;
    private final RoomCandidateRepository roomCandidateRepository;

    @Transactional
    public CreateTravelRoomResponse createRoom(Long memberId, CreateTravelRoomRequest request) {
        validate(memberId, request);
        List<Long> keywordIds = List.copyOf(request.keywordIds());
        CandidateCompositionPlan plan = candidateCompositionService.preparePlan(
                request.regionId(), request.travelDate(), keywordIds);

        TravelRoom room = travelRoomRepository.save(TravelRoom.create(createInviteToken(), memberId,
                normalizeName(request.roomName()), request.travelDate(), request.regionId(), request.courseSpotCount(),
                request.includesFood(), request.includesLodging(), request.includesShopping(),
                plan.conditionKey(), plan.candidateOffset()));
        roomParticipantRepository.save(RoomParticipant.createHost(room.getId(), memberId));
        roomKeywordRepository.saveAll(keywordIds.stream().map(id -> RoomKeyword.create(room.getId(), id)).toList());
        candidateCompositionService.composeCandidates(room.getId(), request.regionId(), request.travelDate(),
                keywordIds, plan.candidateOffset());
        int candidateCount = roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(room.getId()).size();
        return CreateTravelRoomResponse.from(room, keywordIds, candidateCount);
    }

    private void validate(Long memberId, CreateTravelRoomRequest request) {
        if (!referenceQueryRepository.memberExists(memberId))
            throw new InvalidTravelRoomRequestException("회원이 존재하지 않습니다.");
        if (new HashSet<>(request.keywordIds()).size() != request.keywordIds().size())
            throw new InvalidTravelRoomRequestException("키워드는 중복될 수 없습니다.");
        if (!referenceQueryRepository.regionExists(request.regionId()))
            throw new InvalidTravelRoomRequestException("존재하지 않는 자치구입니다.");
        if (referenceQueryRepository.countActiveKeywords(request.keywordIds()) != request.keywordIds().size())
            throw new InvalidTravelRoomRequestException("존재하지 않거나 비활성화된 키워드가 포함되어 있습니다.");
    }

    private String createInviteToken() {
        byte[] bytes = new byte[24];
        String token;
        do {
            SECURE_RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (travelRoomRepository.existsByInviteToken(token));
        return token;
    }

    private String normalizeName(String name) {
        return name == null || name.isBlank() ? null : name.trim();
    }
}
