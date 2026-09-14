package com.tourswitch.domain.course.service;

import com.tourswitch.domain.course.entity.Course;
import com.tourswitch.domain.course.entity.CourseSpot;
import com.tourswitch.domain.course.entity.CourseExtraCandidate;
import com.tourswitch.domain.course.entity.SpotRole;
import com.tourswitch.domain.course.repository.CourseExtraCandidateRepository;
import com.tourswitch.domain.course.repository.CourseRepository;
import com.tourswitch.domain.course.repository.CourseSpotRepository;
import com.tourswitch.domain.course.repository.NearbySpotQueryRepository;
import com.tourswitch.domain.course.repository.NearbySpotRow;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository.RoomSettings;
import com.tourswitch.domain.vote.service.CourseSelectionCandidate;
import com.tourswitch.domain.vote.service.VoteResultQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 투표 종료 시점에 자동으로 코스 초안(DRAFT)을 만든다(계획 문서 5단계, DB설계 8.1절).
 * 득표 상위 course_spot_count곳을 뽑아 방문 순서를 최적화하고, 방장이 켠 부가 카테고리가
 * 있으면 자체 DB의 좌표를 기준으로 각 경유지 주변 후보도 함께 채운다.
 */
@Service
@RequiredArgsConstructor
public class CourseGenerationService {

    private static final int FOOD_CONTENT_TYPE_ID = 39;
    private static final int SHOPPING_CONTENT_TYPE_ID = 38;
    private static final int LODGING_CONTENT_TYPE_ID = 32;
    private static final int EXTRA_CANDIDATE_LIMIT = 5;
    private static final double FOOD_SHOPPING_PRIMARY_RADIUS_METERS = 1000;
    private static final double FOOD_SHOPPING_FALLBACK_RADIUS_METERS = 2000;
    private static final double LODGING_PRIMARY_RADIUS_METERS = 2000;
    private static final double LODGING_FALLBACK_RADIUS_METERS = 3000;

    private final VoteResultQueryService voteResultQueryService;
    private final TravelRoomStatusQueryRepository travelRoomStatusQueryRepository;
    private final NearbySpotQueryRepository nearbySpotQueryRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final CourseExtraCandidateRepository courseExtraCandidateRepository;
    private final TransactionTemplate transactionTemplate;

    public Course generateDraftCourse(Long travelRoomId) {
        Selection selection = readSelection(travelRoomId);
        RouteOptimizer.Result route = RouteOptimizer.findShortestPath(selection.contentIds(),
                buildDistanceMatrix(selection.candidateByContentId()));

        Map<String, Map<SpotRole, List<NearbySpotRow>>> extrasByAnchorContentId = fetchExtraCandidates(
                selection.candidateByContentId(), route.orderedSpotIds(), selection.settings());

        return persist(travelRoomId, selection, route, extrasByAnchorContentId);
    }

    private Selection readSelection(Long travelRoomId) {
        return transactionTemplate.execute(status -> {
            RoomSettings settings = travelRoomStatusQueryRepository.findRoomSettings(travelRoomId);
            List<CourseSelectionCandidate> ranked = voteResultQueryService.getRankedCandidates(travelRoomId);
            List<CourseSelectionCandidate> selected = ranked.stream().limit(settings.courseSpotCount()).toList();
            Map<String, CourseSelectionCandidate> candidateByContentId = selected.stream()
                    .collect(Collectors.toMap(CourseSelectionCandidate::contentId, candidate -> candidate));
            return new Selection(settings, selected, candidateByContentId);
        });
    }

    private Map<String, Map<String, Integer>> buildDistanceMatrix(Map<String, CourseSelectionCandidate> byId) {
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        for (String id : byId.keySet()) {
            matrix.put(id, new HashMap<>());
        }
        List<String> ids = List.copyOf(byId.keySet());
        for (int i = 0; i < ids.size(); i++) {
            CourseSelectionCandidate a = byId.get(ids.get(i));
            for (int j = i + 1; j < ids.size(); j++) {
                CourseSelectionCandidate b = byId.get(ids.get(j));
                int distance = HaversineCalculator.distanceMeters(a.latitude(), a.longitude(), b.latitude(),
                        b.longitude());
                matrix.get(a.contentId()).put(b.contentId(), distance);
                matrix.get(b.contentId()).put(a.contentId(), distance);
            }
        }
        return matrix;
    }

    private Map<String, Map<SpotRole, List<NearbySpotRow>>> fetchExtraCandidates(
            Map<String, CourseSelectionCandidate> candidateByContentId, List<String> orderedSpotIds,
            RoomSettings settings) {
        Map<String, Map<SpotRole, List<NearbySpotRow>>> result = new HashMap<>();
        if (settings.includesFood()) {
            for (String contentId : orderedSpotIds) {
                addExtraCandidates(result, candidateByContentId.get(contentId), SpotRole.FOOD,
                        FOOD_CONTENT_TYPE_ID, FOOD_SHOPPING_PRIMARY_RADIUS_METERS,
                        FOOD_SHOPPING_FALLBACK_RADIUS_METERS);
            }
        }
        if (settings.includesShopping()) {
            for (String contentId : orderedSpotIds) {
                addExtraCandidates(result, candidateByContentId.get(contentId), SpotRole.SHOPPING,
                        SHOPPING_CONTENT_TYPE_ID, FOOD_SHOPPING_PRIMARY_RADIUS_METERS,
                        FOOD_SHOPPING_FALLBACK_RADIUS_METERS);
            }
        }
        if (settings.includesLodging() && !orderedSpotIds.isEmpty()) {
            String lastStopContentId = orderedSpotIds.get(orderedSpotIds.size() - 1);
            addExtraCandidates(result, candidateByContentId.get(lastStopContentId), SpotRole.LODGING,
                    LODGING_CONTENT_TYPE_ID, LODGING_PRIMARY_RADIUS_METERS, LODGING_FALLBACK_RADIUS_METERS);
        }
        return result;
    }

    private void addExtraCandidates(Map<String, Map<SpotRole, List<NearbySpotRow>>> result,
                                     CourseSelectionCandidate anchor, SpotRole role, int contentTypeId,
                                     double primaryRadiusMeters, double fallbackRadiusMeters) {
        List<NearbySpotRow> nearby = nearbySpotQueryRepository.findNearby(anchor.latitude(), anchor.longitude(),
                contentTypeId, primaryRadiusMeters, EXTRA_CANDIDATE_LIMIT);
        if (nearby.size() < EXTRA_CANDIDATE_LIMIT) {
            nearby = nearbySpotQueryRepository.findNearby(anchor.latitude(), anchor.longitude(), contentTypeId,
                    fallbackRadiusMeters, EXTRA_CANDIDATE_LIMIT);
        }
        result.computeIfAbsent(anchor.contentId(), key -> new HashMap<>()).put(role, nearby);
    }

    private Course persist(Long travelRoomId, Selection selection, RouteOptimizer.Result route,
                            Map<String, Map<SpotRole, List<NearbySpotRow>>> extrasByAnchorContentId) {
        return transactionTemplate.execute(status -> {
            Course course = Course.create(travelRoomId, selection.settings().travelDate());
            course.assignTotalDistance(route.totalDistanceMeters());
            courseRepository.save(course);

            List<CourseSpot> courseSpots = new ArrayList<>();
            int visitOrder = 1;
            for (String contentId : route.orderedSpotIds()) {
                CourseSelectionCandidate candidate = selection.candidateByContentId().get(contentId);
                courseSpots.add(CourseSpot.create(course, contentId, SpotRole.ATTRACTION, visitOrder++,
                        candidate.title(), candidate.concentrationRateSnapshot(), (int) candidate.voteCount()));
            }
            courseSpotRepository.saveAll(courseSpots);

            List<CourseExtraCandidate> extras = new ArrayList<>();
            for (CourseSpot anchor : courseSpots) {
                Map<SpotRole, List<NearbySpotRow>> byRole = extrasByAnchorContentId.get(anchor.getContentId());
                if (byRole == null) {
                    continue;
                }
                for (Map.Entry<SpotRole, List<NearbySpotRow>> entry : byRole.entrySet()) {
                    int displayOrder = 1;
                    for (NearbySpotRow row : entry.getValue()) {
                        extras.add(CourseExtraCandidate.create(course, anchor, row.contentId(), entry.getKey(),
                                row.distanceMeters(), displayOrder++));
                    }
                }
            }
            courseExtraCandidateRepository.saveAll(extras);

            return course;
        });
    }

    private record Selection(RoomSettings settings, List<CourseSelectionCandidate> selected,
                              Map<String, CourseSelectionCandidate> candidateByContentId) {
        List<String> contentIds() {
            return selected.stream().map(CourseSelectionCandidate::contentId).toList();
        }
    }
}
