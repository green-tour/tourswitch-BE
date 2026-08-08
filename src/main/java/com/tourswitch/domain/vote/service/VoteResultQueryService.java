package com.tourswitch.domain.vote.service;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.CandidateVoteCount;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.repository.RoomVoteRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 결과를 다른 도메인(코스)에 읽기 전용으로 노출하는 전용 서비스. VoteService와 분리한 이유는
 * 코스 자동 생성이 투표 완료 트랜잭션 안에서 이 결과를 읽어야 하는데, VoteService가 코스 도메인을
 * 다시 호출하는 구조라 VoteService에 두면 두 도메인 서비스가 서로를 참조하는 순환 의존이 생기기
 * 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteResultQueryService {

    private final RoomCandidateRepository roomCandidateRepository;
    private final RoomVoteRepository roomVoteRepository;

    /**
     * 득표수 내림차순, 동률이면 집중률(concentrationRateSnapshot) 오름차순(=crowdEase 높은 순)으로 정렬한다.
     */
    public List<CourseSelectionCandidate> getRankedCandidates(Long travelRoomId) {
        List<RoomCandidate> candidates = roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(
                travelRoomId);
        List<Long> candidateIds = candidates.stream().map(RoomCandidate::getId).toList();

        Map<Long, Long> voteCountByCandidateId = candidateIds.isEmpty()
                ? Map.of()
                : roomVoteRepository.countGroupedByCandidateIds(candidateIds).stream()
                        .collect(Collectors.toMap(CandidateVoteCount::getCandidateId,
                                CandidateVoteCount::getVoteCount));

        Comparator<CourseSelectionCandidate> comparator = Comparator
                .comparing(CourseSelectionCandidate::voteCount, Comparator.reverseOrder())
                .thenComparing(CourseSelectionCandidate::concentrationRateSnapshot,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        return candidates.stream()
                .map(candidate -> new CourseSelectionCandidate(candidate.getId(), candidate.getTouristSpotId(),
                        voteCountByCandidateId.getOrDefault(candidate.getId(), 0L),
                        candidate.getConcentrationRateSnapshot()))
                .sorted(comparator)
                .toList();
    }
}
