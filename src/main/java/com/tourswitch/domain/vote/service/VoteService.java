package com.tourswitch.domain.vote.service;

import com.tourswitch.domain.course.service.CourseGenerationService;
import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.entity.RoomVote;
import com.tourswitch.domain.vote.exception.CandidateNotFoundException;
import com.tourswitch.domain.vote.exception.VoteAccessDeniedException;
import com.tourswitch.domain.vote.exception.VoteSessionNotActiveException;
import com.tourswitch.domain.vote.repository.CandidateVoteCount;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import com.tourswitch.domain.vote.repository.RoomVoteRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository;
import com.tourswitch.domain.vote.response.CandidateTallyResponseDTO;
import com.tourswitch.domain.vote.response.ParticipantStatusResponseDTO;
import com.tourswitch.domain.vote.response.VoteTallyResponseDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계획 문서 4단계. 카드 선택 = 한 표. 취소는 행 삭제로 멱등하게 처리하고, 전원 완료 시
 * travel_room을 자동 종료한다(방장 수동 종료 경로와의 경합은 조건부 UPDATE로 안전하게 처리).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private static final String VOTING_STATUS = "VOTING";

    private final RoomCandidateRepository roomCandidateRepository;
    private final RoomVoteRepository roomVoteRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;
    private final TravelRoomStatusQueryRepository travelRoomStatusQueryRepository;
    private final CourseGenerationService courseGenerationService;

    @Transactional
    public VoteTallyResponseDTO selectCandidate(Long travelRoomId, Long candidateId, Long memberId) {
        requireVotingSession(travelRoomId);
        requireParticipant(travelRoomId, memberId);
        RoomCandidate candidate = requireCandidateInRoom(travelRoomId, candidateId);

        if (roomVoteRepository.findByRoomCandidateIdAndMemberId(candidateId, memberId).isEmpty()) {
            try {
                roomVoteRepository.save(RoomVote.create(candidate, memberId));
                roomVoteRepository.flush();
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 유니크 제약(room_candidate_id, member_id)에 걸린 경우 이미 투표된 것으로 간주한다.
            }
        }

        return buildTally(travelRoomId);
    }

    @Transactional
    public VoteTallyResponseDTO cancelVote(Long travelRoomId, Long candidateId, Long memberId) {
        requireVotingSession(travelRoomId);
        requireParticipant(travelRoomId, memberId);
        requireCandidateInRoom(travelRoomId, candidateId);

        roomVoteRepository.findByRoomCandidateIdAndMemberId(candidateId, memberId)
                .ifPresent(roomVoteRepository::delete);

        return buildTally(travelRoomId);
    }

    @Transactional
    public VoteTallyResponseDTO completeSelection(Long travelRoomId, Long memberId, boolean completed) {
        requireVotingSession(travelRoomId);
        requireParticipant(travelRoomId, memberId);

        roomParticipantQueryRepository.updateSelectionCompletion(travelRoomId, memberId, completed);

        if (completed && roomParticipantQueryRepository.allParticipantsCompleted(travelRoomId)) {
            if (travelRoomStatusQueryRepository.closeIfVoting(travelRoomId)) {
                courseGenerationService.generateDraftCourse(travelRoomId);
            }
        }

        return buildTally(travelRoomId);
    }

    public VoteTallyResponseDTO getTally(Long travelRoomId, Long memberId) {
        requireParticipant(travelRoomId, memberId);
        return buildTally(travelRoomId);
    }

    private VoteTallyResponseDTO buildTally(Long travelRoomId) {
        List<RoomCandidate> candidates = roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(
                travelRoomId);
        List<Long> candidateIds = candidates.stream().map(RoomCandidate::getId).toList();

        Map<Long, Long> voteCountByCandidateId = candidateIds.isEmpty()
                ? Map.of()
                : roomVoteRepository.countGroupedByCandidateIds(candidateIds).stream()
                        .collect(Collectors.toMap(CandidateVoteCount::getCandidateId,
                                CandidateVoteCount::getVoteCount));

        List<CandidateTallyResponseDTO> candidateTallies = candidates.stream()
                .map(candidate -> CandidateTallyResponseDTO.of(candidate.getId(), candidate.getTouristSpotId(),
                        candidate.getDisplayOrder(), voteCountByCandidateId.getOrDefault(candidate.getId(), 0L)))
                .toList();

        List<ParticipantStatusResponseDTO> participantStatuses = roomParticipantQueryRepository
                .findParticipantCompletions(travelRoomId).stream()
                .map(ParticipantStatusResponseDTO::from)
                .toList();

        String roomStatus = travelRoomStatusQueryRepository.findStatus(travelRoomId);
        return VoteTallyResponseDTO.of(roomStatus, candidateTallies, participantStatuses);
    }

    private void requireVotingSession(Long travelRoomId) {
        if (!VOTING_STATUS.equals(travelRoomStatusQueryRepository.findStatus(travelRoomId))) {
            throw new VoteSessionNotActiveException("투표가 종료된 세션입니다.");
        }
    }

    private void requireParticipant(Long travelRoomId, Long memberId) {
        if (!roomParticipantQueryRepository.isParticipant(travelRoomId, memberId)) {
            throw new VoteAccessDeniedException("이 방의 참여자만 이용할 수 있습니다.");
        }
    }

    private RoomCandidate requireCandidateInRoom(Long travelRoomId, Long candidateId) {
        RoomCandidate candidate = roomCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("존재하지 않는 후보 카드입니다."));
        if (!candidate.getTravelRoomId().equals(travelRoomId)) {
            throw new CandidateNotFoundException("이 방에 속하지 않는 후보 카드입니다.");
        }
        return candidate;
    }
}
