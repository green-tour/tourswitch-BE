package com.tourswitch.domain.vote.response;

/**
 * 투표 현황 화면의 실시간 순위에 음식점·숙박·쇼핑을 함께 보여주기 위한 항목.
 * 관광지 후보(room_candidate)와 저장 위치가 달라 별도 목록으로 내려준다.
 */
public record ExtraCandidateTallyResponseDTO(Long candidateId, String contentId, String spotRole, String title,
                                              String imageUrl, Integer distanceMeters, long voteCount,
                                              boolean isSelected, boolean myVote) {
}
