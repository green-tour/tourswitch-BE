package com.tourswitch.domain.place.service;

import com.tourswitch.domain.member.exception.MemberNotFoundException;
import com.tourswitch.domain.member.repository.MemberRepository;
import com.tourswitch.domain.place.provider.FavoritePlaceProvider;
import com.tourswitch.domain.place.repository.PlaceFavoriteRepository;
import com.tourswitch.domain.place.response.FavoritePlaceResponseDTO;
import com.tourswitch.domain.place.response.PlaceFavoriteResponseDTO;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 회원의 관광지 찜 추가, 취소와 상태 조회를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceFavoriteService {

    private final PlaceFavoriteRepository placeFavoriteRepository;
    private final MemberRepository memberRepository;
    private final FavoritePlaceProvider favoritePlaceProvider;

    /**
     * 현재 회원이 찜한 관광지의 최신 TourAPI 정보를 최근 찜 순서로 반환한다.
     * 외부 API 호출이 포함되므로 쓰기 트랜잭션을 열지 않는다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<FavoritePlaceResponseDTO> getFavorites(Long memberId) {
        return placeFavoriteRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(favorite -> favoritePlaceProvider.findByContentId(favorite.getContentId()))
                .flatMap(Optional::stream)
                .map(FavoritePlaceResponseDTO::from)
                .toList();
    }

    /**
     * 현재 회원이 관광지를 찜했는지 반환한다.
     */
    public PlaceFavoriteResponseDTO getFavorite(Long memberId, String contentId) {
        return PlaceFavoriteResponseDTO.of(contentId,
                placeFavoriteRepository.existsByMemberIdAndContentId(memberId, contentId));
    }

    /**
     * 아직 찜하지 않은 관광지를 현재 회원의 찜 목록에 추가한다.
     */
    @Transactional
    public PlaceFavoriteResponseDTO addFavorite(Long memberId, String contentId) {
        requireMember(memberId);
        placeFavoriteRepository.insertIfAbsent(memberId, contentId);
        return PlaceFavoriteResponseDTO.of(contentId, true);
    }

    /**
     * 현재 회원의 관광지 찜을 취소하며 이미 취소된 요청도 성공으로 처리한다.
     */
    @Transactional
    public PlaceFavoriteResponseDTO removeFavorite(Long memberId, String contentId) {
        placeFavoriteRepository.findByMemberIdAndContentId(memberId, contentId)
                .ifPresent(placeFavoriteRepository::delete);
        return PlaceFavoriteResponseDTO.of(contentId, false);
    }

    /**
     * 찜을 생성할 회원이 실제로 존재하는지 검증한다.
     */
    private void requireMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
    }
}
