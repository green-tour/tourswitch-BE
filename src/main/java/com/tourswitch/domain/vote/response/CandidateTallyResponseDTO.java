package com.tourswitch.domain.vote.response;

import java.math.BigDecimal;

/**
 * keywordId는 방 생성 때 고른 키워드로 후보를 거르는 화면 필터에 쓴다.
 * 후보를 구성할 때 room_candidate에 저장한 값을 그대로 내려준다.
 * 혼잡도는 집중률이 없는 관광지가 많아 비어 있을 수 있다.
 */
public record CandidateTallyResponseDTO(Long candidateId, String contentId, String title, String imageUrl,
                                         Long keywordId, Integer displayOrder, long voteCount,
                                         BigDecimal concentrationRate, String concentrationGrade) {

    public static CandidateTallyResponseDTO of(Long candidateId, String contentId, String title, String imageUrl,
                                                Long keywordId, Integer displayOrder, long voteCount,
                                                BigDecimal concentrationRate, String concentrationGrade) {
        return new CandidateTallyResponseDTO(candidateId, contentId, title, imageUrl, keywordId, displayOrder,
                voteCount, concentrationRate, concentrationGrade);
    }
}
