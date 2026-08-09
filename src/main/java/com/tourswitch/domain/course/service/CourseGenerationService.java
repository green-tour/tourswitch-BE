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
import com.tourswitch.domain.course.repository.SpotDistanceQueryRepository;
import com.tourswitch.domain.course.repository.TouristSpotSnapshotQueryRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository;
import com.tourswitch.domain.vote.repository.TravelRoomStatusQueryRepository.RoomSettings;
import com.tourswitch.domain.vote.service.CourseSelectionCandidate;
import com.tourswitch.domain.vote.service.VoteResultQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 종료 시점에 자동으로 코스 초안(DRAFT)을 만든다(계획 문서 5단계, DB설계 8.1절).
 * 득표 상위 course_spot_count곳을 뽑아 방문 순서를 최적화하고, 방장이 켠 부가 카테고리가
 * 있으면 각 경유지 주변의 후보도 함께 채운다.
 */
@Service
@RequiredArgsConstructor
@Transactional
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
    private final SpotDistanceQueryRepository spotDistanceQueryRepository;
    private final NearbySpotQueryRepository nearbySpotQueryRepository;
    private final TouristSpotSnapshotQueryRepository touristSpotSnapshotQueryRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final CourseExtraCandidateRepository courseExtraCandidateRepository;

    public Course generateDraftCourse(Long travelRoomId) {
        RoomSettings settings = travelRoomStatusQueryRepository.findRoomSettings(travelRoomId);

        List<CourseSelectionCandidate> ranked = voteResultQueryService.getRankedCandidates(travelRoomId);
        List<CourseSelectionCandidate> selected = ranked.stream().limit(settings.courseSpotCount()).toList();
        Map<Long, CourseSelectionCandidate> candidateByTouristSpotId = selected.stream()
                .collect(Collectors.toMap(CourseSelectionCandidate::touristSpotId, candidate -> candidate));

        List<Long> touristSpotIds = selected.stream().map(CourseSelectionCandidate::touristSpotId).toList();
        Map<Long, Map<Long, Integer>> distanceMatrix = spotDistanceQueryRepository.findDistanceMatrix(
                touristSpotIds);
        RouteOptimizer.Result route = RouteOptimizer.findShortestPath(touristSpotIds, distanceMatrix);
        Map<Long, String> titles = touristSpotSnapshotQueryRepository.findTitles(touristSpotIds);

        Course course = Course.create(travelRoomId, settings.travelDate());
        course.assignTotalDistance(route.totalDistanceMeters());
        courseRepository.save(course);

        List<CourseSpot> courseSpots = new ArrayList<>();
        int visitOrder = 1;
        for (Long touristSpotId : route.orderedSpotIds()) {
            CourseSelectionCandidate candidate = candidateByTouristSpotId.get(touristSpotId);
            courseSpots.add(CourseSpot.create(course, touristSpotId, SpotRole.ATTRACTION, visitOrder++,
                    titles.get(touristSpotId), candidate.concentrationRateSnapshot(),
                    (int) candidate.voteCount()));
        }
        courseSpotRepository.saveAll(courseSpots);

        generateExtraCandidates(course, courseSpots, settings);

        return course;
    }

    private void generateExtraCandidates(Course course, List<CourseSpot> courseSpots, RoomSettings settings) {
        if (settings.includesFood()) {
            for (CourseSpot anchor : courseSpots) {
                addExtraCandidates(course, anchor, SpotRole.FOOD, FOOD_CONTENT_TYPE_ID,
                        FOOD_SHOPPING_PRIMARY_RADIUS_METERS, FOOD_SHOPPING_FALLBACK_RADIUS_METERS);
            }
        }
        if (settings.includesShopping()) {
            for (CourseSpot anchor : courseSpots) {
                addExtraCandidates(course, anchor, SpotRole.SHOPPING, SHOPPING_CONTENT_TYPE_ID,
                        FOOD_SHOPPING_PRIMARY_RADIUS_METERS, FOOD_SHOPPING_FALLBACK_RADIUS_METERS);
            }
        }
        if (settings.includesLodging() && !courseSpots.isEmpty()) {
            CourseSpot lastStop = courseSpots.get(courseSpots.size() - 1);
            addExtraCandidates(course, lastStop, SpotRole.LODGING, LODGING_CONTENT_TYPE_ID,
                    LODGING_PRIMARY_RADIUS_METERS, LODGING_FALLBACK_RADIUS_METERS);
        }
    }

    private void addExtraCandidates(Course course, CourseSpot anchor, SpotRole role, int contentTypeId,
                                     double primaryRadiusMeters, double fallbackRadiusMeters) {
        List<NearbySpotRow> nearby = nearbySpotQueryRepository.findNearby(anchor.getTouristSpotId(), contentTypeId,
                primaryRadiusMeters, EXTRA_CANDIDATE_LIMIT);
        if (nearby.size() < EXTRA_CANDIDATE_LIMIT) {
            nearby = nearbySpotQueryRepository.findNearby(anchor.getTouristSpotId(), contentTypeId,
                    fallbackRadiusMeters, EXTRA_CANDIDATE_LIMIT);
        }

        List<CourseExtraCandidate> extras = new ArrayList<>();
        int displayOrder = 1;
        for (NearbySpotRow row : nearby) {
            extras.add(CourseExtraCandidate.create(course, anchor, row.touristSpotId(), role, row.distanceMeters(),
                    displayOrder++));
        }
        courseExtraCandidateRepository.saveAll(extras);
    }
}
