package com.tourswitch.domain.congestion.service;

import com.tourswitch.domain.congestion.repository.MapCongestionQueryRepository;
import com.tourswitch.domain.congestion.repository.MapCongestionQueryRepository.MapCongestionRow;
import com.tourswitch.domain.congestion.response.CongestionLegendResponseDTO;
import com.tourswitch.domain.congestion.response.MapAreaCongestionResponseDTO;
import com.tourswitch.domain.congestion.response.MapCongestionResponseDTO;
import com.tourswitch.domain.congestion.response.MapPlaceResponseDTO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapCongestionService {

    private static final int DELAY_MINUTES = 20;
    private static final List<CongestionLegendResponseDTO> LEGEND = List.of(
            new CongestionLegendResponseDTO("여유", "#2EBD85", 1),
            new CongestionLegendResponseDTO("보통", "#F2C94C", 2),
            new CongestionLegendResponseDTO("약간 붐빔", "#F2994A", 3),
            new CongestionLegendResponseDTO("붐빔", "#EB5757", 4)
    );

    private final MapCongestionQueryRepository queryRepository;

    public MapCongestionResponseDTO getCongestionMap() {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, AreaAccumulator> areasById = new LinkedHashMap<>();
        for (MapCongestionRow row : queryRepository.findLatestAreas()) {
            AreaAccumulator area = areasById.computeIfAbsent(row.areaId(), ignored -> new AreaAccumulator(row));
            if (row.contentId() != null) {
                area.places().add(new MapPlaceResponseDTO(
                        row.contentId(), row.touristSpotTitle(), row.firstImageUrl(),
                        row.touristSpotLatitude(), row.touristSpotLongitude(),
                        "/api/places/" + row.contentId()
                ));
            }
        }
        List<MapAreaCongestionResponseDTO> areas = areasById.values().stream()
                .map(area -> area.toResponse(now))
                .toList();
        int delayedAreaCount = (int) areas.stream().filter(MapAreaCongestionResponseDTO::delayed).count();
        return new MapCongestionResponseDTO(now, delayedAreaCount, LEGEND, areas);
    }

    private static String color(String level) {
        if (level == null) {
            return "#9E9E9E";
        }
        return LEGEND.stream()
                .filter(item -> item.level().equals(level))
                .map(CongestionLegendResponseDTO::color)
                .findFirst()
                .orElse("#9E9E9E");
    }

    private record AreaAccumulator(MapCongestionRow row, List<MapPlaceResponseDTO> places) {

        private AreaAccumulator(MapCongestionRow row) {
            this(row, new ArrayList<>());
        }

        private MapAreaCongestionResponseDTO toResponse(LocalDateTime now) {
            boolean delayed = row.collectedAt() == null
                    || Duration.between(row.collectedAt(), now).toMinutes() > DELAY_MINUTES;
            return new MapAreaCongestionResponseDTO(
                    row.areaId(), row.areaCode(), row.areaName(), row.category(),
                    row.areaLatitude(), row.areaLongitude(), row.congestionLevel(),
                    color(row.congestionLevel()), row.congestionMessage(),
                    row.populationMin(), row.populationMax(), row.observedAt(), row.collectedAt(),
                    delayed, List.copyOf(places)
            );
        }
    }
}
