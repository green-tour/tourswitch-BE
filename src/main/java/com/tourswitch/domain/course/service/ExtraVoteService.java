package com.tourswitch.domain.course.service;

import com.tourswitch.domain.course.entity.CourseExtraCandidate;
import com.tourswitch.domain.course.entity.CourseExtraVote;
import com.tourswitch.domain.course.repository.CourseExtraCandidateQueryRepository;
import com.tourswitch.domain.course.repository.CourseExtraCandidateQueryRepository.ExtraCandidateRow;
import com.tourswitch.domain.course.repository.CourseExtraCandidateRepository;
import com.tourswitch.domain.course.repository.CourseExtraVoteRepository;
import com.tourswitch.domain.course.response.ExtraCandidateResponseDTO;
import com.tourswitch.domain.course.response.ExtraVoteResponseDTO;
import com.tourswitch.domain.vote.exception.VoteAccessDeniedException;
import com.tourswitch.domain.vote.exception.VoteSessionNotActiveException;
import com.tourswitch.domain.vote.repository.RoomParticipantQueryRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추가 투표 라운드(음식점·숙박·쇼핑). 관광지 투표가 끝나 경유지가 확정된 뒤에만 열린다.
 * 라운드가 끝나면 역할별 최다 득표 한 곳을 확정해 코스에 반영한다.
 */
@Service
@RequiredArgsConstructor
public class ExtraVoteService {

    private static final String EXTRA_VOTING_STATUS = "EXTRA_VOTING";

    private final CourseExtraCandidateQueryRepository courseExtraCandidateQueryRepository;
    private final CourseExtraCandidateRepository courseExtraCandidateRepository;
    private final CourseExtraVoteRepository courseExtraVoteRepository;
    private final RoomParticipantQueryRepository roomParticipantQueryRepository;
    private final TravelRoomStatusQueryRepository travelRoomStatusQueryRepository;

    public ExtraVoteResponseDTO getCandidates(Long travelRoomId, Long memberId) {
        requireHost(travelRoomId, memberId);
        return buildResponse(travelRoomId, memberId);
    }

    @Transactional
    public ExtraVoteResponseDTO vote(Long travelRoomId, Long candidateId, Long memberId) {
        requireExtraVotingSession(travelRoomId);
        requireHost(travelRoomId, memberId);
        requireCandidateInRoom(candidateId, travelRoomId);

        if (courseExtraVoteRepository.findByCourseExtraCandidateIdAndMemberId(candidateId, memberId).isEmpty()) {
            courseExtraVoteRepository.save(CourseExtraVote.create(candidateId, memberId));
        }
        return buildResponse(travelRoomId, memberId);
    }

    @Transactional
    public ExtraVoteResponseDTO cancelVote(Long travelRoomId, Long candidateId, Long memberId) {
        requireExtraVotingSession(travelRoomId);
        requireHost(travelRoomId, memberId);
        requireCandidateInRoom(candidateId, travelRoomId);

        courseExtraVoteRepository.findByCourseExtraCandidateIdAndMemberId(candidateId, memberId)
                .ifPresent(courseExtraVoteRepository::delete);
        return buildResponse(travelRoomId, memberId);
    }

    /** 방장이 추가 투표를 완료하면 결과를 확정해 추천 코스를 바로 볼 수 있게 한다. */
    @Transactional
    public ExtraVoteResponseDTO complete(Long travelRoomId, Long memberId) {
        requireExtraVotingSession(travelRoomId);
        requireHost(travelRoomId, memberId);

        roomParticipantQueryRepository.updateExtraSelectionCompletion(travelRoomId, memberId, true);
        closeExtraVotingRound(travelRoomId);
        return buildResponse(travelRoomId, memberId);
    }

    /**
     * 역할별 최다 득표 한 곳을 확정하고 방을 닫는다.
     * 방장의 수동 종료 요청이 성공한 경우에만 확정한다.
     */
    @Transactional
    public void closeExtraVotingRound(Long travelRoomId) {
        if (!travelRoomStatusQueryRepository.closeIfExtraVoting(travelRoomId)) {
            return;
        }
        List<Long> winnerIds = courseExtraCandidateQueryRepository.findWinnerIdsByRole(travelRoomId);
        for (Long winnerId : winnerIds) {
            courseExtraCandidateRepository.findById(winnerId).ifPresent(CourseExtraCandidate::select);
        }
    }

    private ExtraVoteResponseDTO buildResponse(Long travelRoomId, Long memberId) {
        List<ExtraCandidateRow> rows = courseExtraCandidateQueryRepository.findByTravelRoomId(travelRoomId, memberId);
        Map<String, List<ExtraCandidateResponseDTO>> byRole = rows.stream()
                .collect(Collectors.groupingBy(ExtraCandidateRow::spotRole, LinkedHashMap::new,
                        Collectors.mapping(row -> new ExtraCandidateResponseDTO(row.id(), row.contentId(),
                                row.title(), row.imageUrl(), row.distanceMeters(), row.displayOrder(),
                                row.voteCount(), row.myVote()), Collectors.toList())));
        return new ExtraVoteResponseDTO(travelRoomStatusQueryRepository.findStatus(travelRoomId), byRole);
    }

    private void requireExtraVotingSession(Long travelRoomId) {
        if (!EXTRA_VOTING_STATUS.equals(travelRoomStatusQueryRepository.findStatus(travelRoomId))) {
            throw new VoteSessionNotActiveException("추가 투표가 진행 중이 아닙니다.");
        }
    }

    private void requireParticipant(Long travelRoomId, Long memberId) {
        if (!roomParticipantQueryRepository.isParticipant(travelRoomId, memberId)) {
            throw new VoteAccessDeniedException("이 방의 참여자가 아닙니다.");
        }
    }

    private void requireHost(Long travelRoomId, Long memberId) {
        if (!roomParticipantQueryRepository.isHost(travelRoomId, memberId)) {
            throw new VoteAccessDeniedException("방장만 추가 투표를 진행할 수 있습니다.");
        }
    }

    private void requireCandidateInRoom(Long candidateId, Long travelRoomId) {
        if (!courseExtraCandidateQueryRepository.belongsToRoom(candidateId, travelRoomId)) {
            throw new VoteSessionNotActiveException("이 방의 부가 후보가 아닙니다.");
        }
    }
}
