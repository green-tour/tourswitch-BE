package com.tourswitch.domain.realtimechange.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 선택 행정동 대표 좌표 기준 3km 이내에서 여행방 키워드 합집합과 일치하는 관광지를 조회한다.
 * 실시간 혼잡도는 원본 관측 시각이 60분 이내인 최신 스냅샷만 사용한다.
 */
@Repository
public class ReplacementCandidateQueryRepository {

    private static final List<Integer> ATTRACTION_CONTENT_TYPE_IDS = List.of(12, 14, 15, 28);

    @PersistenceContext
    private EntityManager entityManager;

    public List<ReplacementCandidateRow> findCandidates(Long courseId, Long administrativeDongId, int limit) {
        return executeCandidateQuery(courseId, administrativeDongId, null, limit);
    }

    public Optional<ReplacementCandidateRow> findEligibleCandidate(Long courseId, Long administrativeDongId,
                                                                    Long touristSpotId) {
        return executeCandidateQuery(courseId, administrativeDongId, touristSpotId, 1).stream().findFirst();
    }

    @SuppressWarnings("unchecked")
    private List<ReplacementCandidateRow> executeCandidateQuery(Long courseId, Long administrativeDongId,
                                                                 Long touristSpotId, int limit) {
        String spotFilter = touristSpotId == null ? "" : " AND ts.id = :touristSpotId ";
        String sql = """
                SELECT ts.id,
                       ts.title,
                       ts.address,
                       CAST(ST_Distance_Sphere(
                           ts.location_point,
                           ST_SRID(POINT(ad.center_longitude, ad.center_latitude), 4326)
                       ) AS SIGNED) AS distance_meters,
                       GROUP_CONCAT(DISTINCT k.keyword_name ORDER BY k.display_order SEPARATOR '||') AS keywords,
                       population.congestion_level,
                       population.observed_at,
                       COALESCE(demand.participant_count, 0) AS participant_count,
                       COUNT(DISTINCT rk.keyword_id) AS matched_keyword_count
                FROM course c
                JOIN travel_room tr ON tr.id = c.travel_room_id
                JOIN administrative_dong ad ON ad.id = :administrativeDongId AND ad.is_active = TRUE
                JOIN room_keyword rk ON rk.travel_room_id = tr.id
                JOIN spot_keyword_link skl ON skl.keyword_id = rk.keyword_id
                JOIN keyword k ON k.id = rk.keyword_id AND k.is_active = TRUE
                JOIN tourist_spot ts ON ts.id = skl.tourist_spot_id
                LEFT JOIN spot_area_link sal
                       ON sal.tourist_spot_id = ts.id AND sal.is_primary = TRUE
                LEFT JOIN seoul_realtime_population population
                       ON population.id = (
                           SELECT latest.id
                           FROM seoul_realtime_population latest
                           WHERE latest.seoul_realtime_area_id = sal.seoul_realtime_area_id
                             AND latest.observed_at >= NOW() - INTERVAL 60 MINUTE
                           ORDER BY latest.observed_at DESC, latest.id DESC
                           LIMIT 1
                       )
                LEFT JOIN spot_daily_demand demand
                       ON demand.tourist_spot_id = ts.id AND demand.target_date = c.travel_date
                WHERE c.id = :courseId
                  AND ts.is_active = TRUE
                  AND ts.is_coordinate_valid = TRUE
                  AND ts.content_type_id IN (:contentTypeIds)
                  AND NOT EXISTS (
                      SELECT 1 FROM course_spot existing
                      WHERE existing.course_id = c.id AND existing.tourist_spot_id = ts.id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM spot_duplicate_link duplicate
                      WHERE duplicate.tourist_spot_id = ts.id
                  )
                """ + spotFilter + """
                GROUP BY ts.id, ts.title, ts.address, ts.location_point,
                         population.congestion_level, population.observed_at,
                         demand.participant_count, ad.center_longitude, ad.center_latitude
                HAVING distance_meters <= 3000
                ORDER BY
                    CASE population.congestion_level
                        WHEN '여유' THEN 1
                        WHEN '보통' THEN 2
                        WHEN '약간 붐빔' THEN 3
                        WHEN '붐빔' THEN 4
                        ELSE 5
                    END ASC,
                    participant_count ASC,
                    matched_keyword_count DESC,
                    distance_meters ASC,
                    ts.id ASC
                """;

        var query = entityManager.createNativeQuery(sql)
                .setParameter("courseId", courseId)
                .setParameter("administrativeDongId", administrativeDongId)
                .setParameter("contentTypeIds", ATTRACTION_CONTENT_TYPE_IDS)
                .setMaxResults(limit);
        if (touristSpotId != null) {
            query.setParameter("touristSpotId", touristSpotId);
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toRow).toList();
    }

    private ReplacementCandidateRow toRow(Object[] row) {
        String keywordText = (String) row[4];
        List<String> keywords = keywordText == null || keywordText.isBlank()
                ? List.of()
                : Arrays.asList(keywordText.split("\\|\\|"));
        return new ReplacementCandidateRow(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).intValue(),
                keywords,
                (String) row[5],
                (LocalDateTime) row[6]);
    }
}
