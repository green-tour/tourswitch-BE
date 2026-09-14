package com.tourswitch.domain.realtimechange.repository;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.realtimechange.entity.AdministrativeDong;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReplacementCandidateQueryRepository {

    private static final int SEARCH_RADIUS_METERS = 3_000;

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ReplacementCandidateRow> findCandidates(Course course, AdministrativeDong dong, int limit) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT tourist_spot.content_id, tourist_spot.title, tourist_spot.address,
                       tourist_spot.first_image_url,
                       ROUND(ST_Distance_Sphere(
                           tourist_spot.location_point,
                           ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                               4326, 'axis-order=long-lat')
                       )) AS distance_meters,
                       keyword.keyword_name,
                       COALESCE(population.congestion_level, forecast.concentration_grade) AS crowd_grade,
                       population.observed_at
                FROM tourist_spot tourist_spot
                JOIN spot_keyword_link keyword_link
                  ON keyword_link.tourist_spot_id = tourist_spot.id
                JOIN keyword keyword
                  ON keyword.id = keyword_link.keyword_id AND keyword.is_active = TRUE
                JOIN room_keyword room_keyword
                  ON room_keyword.keyword_id = keyword.id
                 AND room_keyword.travel_room_id = :travelRoomId
                LEFT JOIN spot_area_link area_link
                  ON area_link.tourist_spot_id = tourist_spot.id AND area_link.is_primary = TRUE
                LEFT JOIN seoul_realtime_population population
                  ON population.id = (
                    SELECT latest_population.id
                    FROM seoul_realtime_population latest_population
                    WHERE latest_population.seoul_realtime_area_id = area_link.seoul_realtime_area_id
                    ORDER BY latest_population.observed_at DESC, latest_population.id DESC
                    LIMIT 1
                  )
                LEFT JOIN spot_crowd_link crowd_link
                  ON crowd_link.tourist_spot_id = tourist_spot.id
                LEFT JOIN spot_crowd_forecast forecast
                  ON forecast.attraction_name = crowd_link.attraction_name
                 AND forecast.district_code = crowd_link.district_code
                 AND forecast.forecast_date = :travelDate
                WHERE tourist_spot.content_type_id IN (12, 14, 15, 28)
                  AND tourist_spot.is_active = TRUE
                  AND tourist_spot.is_coordinate_valid = TRUE
                  AND NOT EXISTS (
                    SELECT 1 FROM course_spot existing_spot
                    WHERE existing_spot.course_id = :courseId
                      AND existing_spot.content_id = tourist_spot.content_id
                  )
                  AND NOT EXISTS (
                    SELECT 1 FROM spot_duplicate_link duplicate_link
                    WHERE duplicate_link.tourist_spot_id = tourist_spot.id
                  )
                  AND ST_Distance_Sphere(
                        tourist_spot.location_point,
                        ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                            4326, 'axis-order=long-lat')
                      ) <= :radiusMeters
                ORDER BY tourist_spot.id, keyword.display_order, keyword.id
                """)
                .setParameter("longitude", dong.getCenterLongitude())
                .setParameter("latitude", dong.getCenterLatitude())
                .setParameter("travelRoomId", course.getTravelRoomId())
                .setParameter("travelDate", course.getTravelDate())
                .setParameter("courseId", course.getId())
                .setParameter("radiusMeters", SEARCH_RADIUS_METERS)
                .getResultList();

        return groupRows(rows).stream()
                .sorted(Comparator.comparingInt(this::crowdRank)
                        .thenComparing(Comparator.comparingInt(
                                (ReplacementCandidateRow row) -> row.matchedKeywords().size()).reversed())
                        .thenComparingInt(ReplacementCandidateRow::distanceMeters)
                        .thenComparing(ReplacementCandidateRow::contentId))
                .limit(limit)
                .toList();
    }

    public Optional<ReplacementCandidateRow> findEligibleCandidate(Course course, AdministrativeDong dong,
                                                                    String contentId) {
        return findCandidates(course, dong, 50).stream()
                .filter(candidate -> candidate.contentId().equals(contentId))
                .findFirst();
    }

    private List<ReplacementCandidateRow> groupRows(List<Object[]> rows) {
        Map<String, CandidateAccumulator> candidates = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String contentId = (String) row[0];
            CandidateAccumulator candidate = candidates.computeIfAbsent(contentId, ignored ->
                    new CandidateAccumulator(
                            contentId,
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            ((Number) row[4]).intValue(),
                            (String) row[6],
                            toLocalDateTime(row[7])
                    ));
            candidate.keywordNames().add((String) row[5]);
        }
        return candidates.values().stream()
                .map(candidate -> new ReplacementCandidateRow(
                        candidate.contentId(),
                        candidate.title(),
                        candidate.address(),
                        candidate.imageUrl(),
                        candidate.distanceMeters(),
                        List.copyOf(candidate.keywordNames()),
                        candidate.crowdGrade(),
                        candidate.observedAt()
                ))
                .toList();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((java.sql.Timestamp) value).toLocalDateTime();
    }

    private int crowdRank(ReplacementCandidateRow row) {
        return switch (row.crowdGrade() == null ? "" : row.crowdGrade()) {
            case "여유" -> 1;
            case "보통" -> 2;
            case "약간 붐빔" -> 3;
            case "붐빔" -> 4;
            default -> 5;
        };
    }

    private record CandidateAccumulator(
            String contentId,
            String title,
            String address,
            String imageUrl,
            int distanceMeters,
            String crowdGrade,
            LocalDateTime observedAt,
            List<String> keywordNames
    ) {
        private CandidateAccumulator(String contentId, String title, String address, String imageUrl,
                                     int distanceMeters, String crowdGrade, LocalDateTime observedAt) {
            this(contentId, title, address, imageUrl, distanceMeters, crowdGrade, observedAt, new ArrayList<>());
        }
    }
}
