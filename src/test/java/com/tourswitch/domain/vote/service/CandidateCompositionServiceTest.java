package com.tourswitch.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tourswitch.domain.vote.entity.RoomCandidate;
import com.tourswitch.domain.vote.repository.RoomCandidateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * spot_keyword_link 매칭 배치(투어스위치_spot_keyword_link_배치.sql) 실행 후의 실제 데이터로
 * 후보 구성 파이프라인(라운드로빈 배분 + 순환 윈도우 + 점수 계산)이 끝까지 동작하는지 확인한다.
 * 종로구(region_id=1)의 전시·박물관/역사유적 키워드는 각각 100건, 50건 이상 매칭돼 있어
 * 라운드로빈이 실제로 두 키워드를 섞어 배분하는지도 함께 검증한다.
 * 트랜잭션 롤백으로 정리하므로 방/회원 등 테스트에서 만든 행은 남지 않는다.
 */
@SpringBootTest
@Transactional
@Rollback
class CandidateCompositionServiceTest {

    private static final Long REGION_ID = 1L;
    private static final List<Long> KEYWORD_IDS = List.of(1L, 4L);
    private static final int MAX_CANDIDATES = 20;
    private static final LocalDate TRAVEL_DATE = LocalDate.of(2026, 7, 28);

    @Autowired
    private CandidateCompositionService candidateCompositionService;

    @Autowired
    private RoomCandidateRepository roomCandidateRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 실제_매칭_데이터로_후보를_구성하고_키워드를_섞어_배분한다() {
        Long memberId = insertTestMember();
        Long travelRoomId = insertTestTravelRoom(memberId);

        candidateCompositionService.composeCandidates(travelRoomId, REGION_ID, TRAVEL_DATE, KEYWORD_IDS);

        List<RoomCandidate> candidates = roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(
                travelRoomId);

        assertThat(candidates).hasSize(MAX_CANDIDATES);
        assertThat(candidates).extracting(RoomCandidate::getDisplayOrder)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, MAX_CANDIDATES).boxed().collect(Collectors.toList()));
        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getRecommendationScore()).isNotNull());
        assertThat(candidates).extracting(RoomCandidate::getKeywordId).contains(1L, 4L);
    }

    private Long insertTestMember() {
        entityManager.createNativeQuery("""
                INSERT INTO member (login_id, password_hash, nickname, status, created_at)
                VALUES ('smoke_test_hongdaewoon', 'x', '스모크테스트', 'ACTIVE', NOW())
                """).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private Long insertTestTravelRoom(Long hostMemberId) {
        entityManager.createNativeQuery("""
                INSERT INTO travel_room
                    (invite_token, host_member_id, room_name, travel_date, region_id, course_spot_count,
                     includes_food, includes_lodging, includes_shopping, status,
                     recommendation_condition_key, candidate_offset, created_at)
                VALUES
                    ('smoke_test_invite_token_0001', :hostMemberId, 'STAGE3_SMOKE_TEST', :travelDate, :regionId, 3,
                     FALSE, FALSE, FALSE, 'VOTING', REPEAT('0', 64), 0, NOW())
                """)
                .setParameter("hostMemberId", hostMemberId)
                .setParameter("travelDate", TRAVEL_DATE)
                .setParameter("regionId", REGION_ID)
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }
}
