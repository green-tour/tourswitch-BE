package com.tourswitch.domain.vote.service;

import com.tourswitch.domain.vote.response.ExtraCandidateTallyResponseDTO;
import com.tourswitch.domain.course.repository.CourseExtraCandidateQueryRepository;
import com.tourswitch.domain.course.service.CourseGenerationService;
import com.tourswitch.domain.course.service.DraftCourseResetService;
import com.tourswitch.domain.course.service.ExtraVoteService;
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
 * 계획 문서 4단계. 카드 선택 = 한 표. 취소는 행 삭제로 멱등하게 처리한다.
 * 투표 라운드 전환과 종료는 방장의 수동 종료만으로 수행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private static final String VOTING_STATUS = "VOTING";
    private static final String EXTRA_VOTING_STATUS = "EXTRA_VOTING";
    private static final String CLOSED_STATUS = "CLOSED";

    private final RoomCandidateRepository roomCandidateRepository;
    private final RoomVoteRepository roomVoteRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;
    private final TravelRoomStatusQueryRepository travelRoomStatusQueryRepository;
    private final CourseGenerationService courseGenerationService;
    private final CourseExtraCandidateQueryRepository courseExtraCandidateQueryRepository;
    private final ExtraVoteService extraVoteService;
    private final DraftCourseResetService draftCourseResetService;

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

        return buildTally(travelRoomId, memberId);
    }

    @Transactional
    public VoteTallyResponseDTO cancelVote(Long travelRoomId, Long candidateId, Long memberId) {
        requireVotingSession(travelRoomId);
        requireParticipant(travelRoomId, memberId);
        requireCandidateInRoom(travelRoomId, candidateId);

        roomVoteRepository.findByRoomCandidateIdAndMemberId(candidateId, memberId)
                .ifPresent(roomVoteRepository::delete);

        return buildTally(travelRoomId, memberId);
    }

    @Transactional
    public VoteTallyResponseDTO completeSelection(Long travelRoomId, Long memberId, boolean completed) {
        requireVotingSession(travelRoomId);
        requireParticipant(travelRoomId, memberId);

        roomParticipantQueryRepository.updateSelectionCompletion(travelRoomId, memberId, completed);

        return buildTally(travelRoomId, memberId);
    }

    /**
     * 코스 확정 전에는 어떤 참여자든 1차 투표를 다시 열 수 있다.
     * CLOSED였다면 이전 투표 결과를 바탕으로 만든 초안/부가 후보는 더 이상 유효하지 않아 함께 초기화한다.
     */
    @Transactional
    public VoteTallyResponseDTO startRevote(Long travelRoomId, Long memberId) {
        requireParticipant(travelRoomId, memberId);
        String status = travelRoomStatusQueryRepository.findStatus(travelRoomId);

        if (VOTING_STATUS.equals(status)) {
            roomParticipantQueryRepository.updateSelectionCompletion(travelRoomId, memberId, false);
            return buildTally(travelRoomId, memberId);
        }
        if (!CLOSED_STATUS.equals(status)) {
            throw new VoteSessionNotActiveException("코스가 확정된 뒤에는 재투표할 수 없습니다.");
        }

        draftCourseResetService.deleteDraftForRoom(travelRoomId);
        roomParticipantQueryRepository.resetRoundCompletions(travelRoomId);
        if (!travelRoomStatusQueryRepository.reopenVotingIfClosed(travelRoomId)) {
            throw new VoteSessionNotActiveException("투표 상태가 변경되어 재투표를 시작할 수 없습니다.");
        }
        return buildTally(travelRoomId, memberId);
    }

    /**
     * 관광지 투표를 끝낸다. 득표를 집계해 경유지를 확정하고 그 좌표로 부가 후보를 만든다.
     * 부가 옵션을 켠 방은 추가 투표 라운드로 넘어가고, 아니면 바로 닫힌다.
     *
     * 방장의 수동 종료 요청이 성공한 경우에만 코스를 만든다.
     */
    @Transactional
    public void closeVotingRound(Long travelRoomId) {
        if (!travelRoomStatusQueryRepository.startExtraVotingIfVoting(travelRoomId)) {
            return;
        }
        courseGenerationService.generateDraftCourse(travelRoomId);
        if (!courseExtraCandidateQueryRepository.existsByTravelRoomId(travelRoomId)) {
            travelRoomStatusQueryRepository.closeIfExtraVoting(travelRoomId);
        }
    }

    /**
     * 방장이 현재 라운드를 강제로 끝낸다. 진행 중인 라운드에 맞춰 다음 단계로 넘긴다.
     */
    @Transactional
    public VoteTallyResponseDTO closeCurrentRoundByHost(Long travelRoomId, Long memberId) {
        if (!roomParticipantQueryRepository.isHost(travelRoomId, memberId)) {
            throw new VoteAccessDeniedException("방장만 투표를 종료할 수 있습니다.");
        }
        String status = travelRoomStatusQueryRepository.findStatus(travelRoomId);
        if (VOTING_STATUS.equals(status)) {
            closeVotingRound(travelRoomId);
        } else if (EXTRA_VOTING_STATUS.equals(status)) {
            extraVoteService.closeExtraVotingRound(travelRoomId);
        } else {
            throw new VoteSessionNotActiveException("이미 종료된 세션입니다.");
        }
        return buildTally(travelRoomId, memberId);
    }

    public VoteTallyResponseDTO getTally(Long travelRoomId, Long memberId) {
        requireParticipant(travelRoomId, memberId);
        return buildTally(travelRoomId, memberId);
    }

    private VoteTallyResponseDTO buildTally(Long travelRoomId, Long memberId) {
        List<RoomCandidate> candidates = roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(
                travelRoomId);
        List<Long> candidateIds = candidates.stream().map(RoomCandidate::getId).toList();

        Map<Long, Long> voteCountByCandidateId = candidateIds.isEmpty()
                ? Map.of()
                : roomVoteRepository.countGroupedByCandidateIds(candidateIds).stream()
                        .collect(Collectors.toMap(CandidateVoteCount::getCandidateId,
                                CandidateVoteCount::getVoteCount));

        List<CandidateTallyResponseDTO> candidateTallies = candidates.stream()
                .map(candidate -> CandidateTallyResponseDTO.of(candidate.getId(), candidate.getContentId(),
                        candidate.getTitleSnapshot(), candidate.getImageUrlSnapshot(), candidate.getKeywordId(),
                        candidate.getDisplayOrder(),
                        voteCountByCandidateId.getOrDefault(candidate.getId(), 0L),
                        candidate.getConcentrationRateSnapshot(), candidate.getConcentrationGradeSnapshot()))
                .toList();

        List<ParticipantStatusResponseDTO> participantStatuses = roomParticipantQueryRepository
                .findParticipantCompletions(travelRoomId).stream()
                .map(ParticipantStatusResponseDTO::from)
                .toList();

        // 추가 투표 라운드의 부가 후보도 같은 화면의 실시간 순위에 올린다.
        // 라운드 전에는 후보가 없어 빈 목록이 된다.
        List<ExtraCandidateTallyResponseDTO> extraCandidateTallies = courseExtraCandidateQueryRepository
                .findByTravelRoomId(travelRoomId, memberId).stream()
                .map(row -> new ExtraCandidateTallyResponseDTO(row.id(), row.contentId(), row.spotRole(),
                        row.title(), row.imageUrl(), row.distanceMeters(), row.voteCount(), row.isSelected(), row.myVote()))
                .toList();

        String roomStatus = travelRoomStatusQueryRepository.findStatus(travelRoomId);
        return VoteTallyResponseDTO.of(roomStatus, candidateTallies, extraCandidateTallies, participantStatuses);
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
