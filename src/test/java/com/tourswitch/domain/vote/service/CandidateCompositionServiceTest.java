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
 * 자체 DB에 적재된 관광지·키워드 링크를 이용해 후보 구성 파이프라인이 끝까지 동작하는지 확인한다.
 * 외부 또는 개발 DB의 기존 데이터에 의존하지 않도록 필요한 관광지와 링크를 직접 준비한다.
 */
@SpringBootTest
@Transactional
@Rollback
class CandidateCompositionServiceTest {

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
        Long regionId = findRegionId();
        List<Long> keywordIds = List.of(findKeywordId("전시·박물관"), findKeywordId("역사유적"));
        insertTouristSpots(regionId, keywordIds);
        Long memberId = insertTestMember();
        Long travelRoomId = insertTestTravelRoom(memberId, regionId);

        candidateCompositionService.composeCandidates(travelRoomId, regionId, TRAVEL_DATE, keywordIds);

        List<RoomCandidate> candidates = roomCandidateRepository.findByTravelRoomIdOrderByDisplayOrderAsc(
                travelRoomId);

        assertThat(candidates).hasSize(MAX_CANDIDATES);
        assertThat(candidates).extracting(RoomCandidate::getDisplayOrder)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, MAX_CANDIDATES).boxed().collect(Collectors.toList()));
        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getRecommendationScore()).isNotNull());
        assertThat(candidates).extracting(RoomCandidate::getKeywordId).containsAll(keywordIds);
    }

    private Long insertTestMember() {
        entityManager.createNativeQuery("""
                INSERT INTO member (social_provider, social_id, nickname, status, created_at)
                VALUES ('KAKAO', 'smoke_test_hongdaewoon', '스모크테스트', 'ACTIVE', NOW())
                """).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private Long insertTestTravelRoom(Long hostMemberId, Long regionId) {
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
                .setParameter("regionId", regionId)
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private Long findRegionId() {
        return ((Number) entityManager.createNativeQuery("""
                SELECT id FROM region WHERE district_code = '11110'
                """).getSingleResult()).longValue();
    }

    private Long findKeywordId(String keywordName) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT id FROM keyword WHERE keyword_name = :keywordName
                """)
                .setParameter("keywordName", keywordName)
                .getSingleResult()).longValue();
    }

    private void insertTouristSpots(Long regionId, List<Long> keywordIds) {
        for (int index = 1; index <= MAX_CANDIDATES; index++) {
            String contentId = "candidate_fixture_" + index;
            entityManager.createNativeQuery("""
                    INSERT INTO tourist_spot
                      (content_id, content_type_id, title, normalized_title, address, normalized_address,
                       latitude, longitude, location_point, classification_level1_code,
                       classification_level2_code, classification_level3_code, region_id,
                       is_coordinate_valid, has_crowd_data, is_active, data_synced_at)
                    VALUES (:contentId, 12, :title, :normalizedTitle, '서울 종로구', '서울종로구',
                            37.57, 126.98,
                            ST_GeomFromText('POINT(126.98 37.57)', 4326, 'axis-order=long-lat'),
                            'HS', 'HS01', 'HS0101', :regionId, TRUE, FALSE, TRUE, UTC_TIMESTAMP())
                    """)
                    .setParameter("contentId", contentId)
                    .setParameter("title", "후보 관광지 " + index)
                    .setParameter("normalizedTitle", "후보관광지" + index)
                    .setParameter("regionId", regionId)
                    .executeUpdate();
            Long touristSpotId = ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                    .getSingleResult()).longValue();
            entityManager.createNativeQuery("""
                    INSERT INTO spot_keyword_link (tourist_spot_id, keyword_id)
                    VALUES (:touristSpotId, :keywordId)
                    """)
                    .setParameter("touristSpotId", touristSpotId)
                    .setParameter("keywordId", keywordIds.get((index - 1) % keywordIds.size()))
                    .executeUpdate();
        }
    }
}
