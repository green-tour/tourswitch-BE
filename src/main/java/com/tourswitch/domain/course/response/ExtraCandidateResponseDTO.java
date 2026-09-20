package com.tourswitch.domain.course.response;

/**
 * 추가 투표 화면의 카드 한 장. 설명은 목록에 없어 사용자가 상세를 열 때 따로 조회한다.
 */
public record ExtraCandidateResponseDTO(
        Long candidateId,
        String contentId,
        String title,
        String imageUrl,
        Integer distanceMeters,
        Integer displayOrder,
        long voteCount,
        boolean myVote
) {
}
