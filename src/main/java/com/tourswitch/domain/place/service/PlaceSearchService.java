package com.tourswitch.domain.place.service;

import com.tourswitch.domain.metadata.model.KeywordCode;
import com.tourswitch.domain.place.exception.PlaceNotFoundException;
import com.tourswitch.domain.place.exception.RegionNotFoundException;
import com.tourswitch.domain.place.repository.PlaceKeywordClassificationQueryRepository;
import com.tourswitch.domain.place.repository.PlaceQueryRepository;
import com.tourswitch.domain.place.repository.PlaceQueryRepository.PlaceDetailRow;
import com.tourswitch.domain.place.repository.PlaceQueryRepository.PlaceForecastRow;
import com.tourswitch.domain.place.response.PlaceAccessibilityResponseDTO;
import com.tourswitch.domain.place.response.PlaceCongestionResponseDTO;
import com.tourswitch.domain.place.response.PlaceDetailResponseDTO;
import com.tourswitch.domain.place.response.PlaceForecastResponseDTO;
import com.tourswitch.domain.place.response.PlaceRealtimeCongestionResponseDTO;
import com.tourswitch.domain.place.response.PlaceSummaryResponseDTO;
import com.tourswitch.global.response.PageRes;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceSearchService {

    private static final String NO_INFORMATION = "제공 정보 없음";
    private static final String DEFAULT_IMAGE_URL = "/images/place-placeholder.svg";
    private static final String DATA_SOURCE = "한국관광공사 TourAPI·서울 실시간 도시데이터";
    private static final int DEFAULT_FORECAST_DAYS = 7;
    private static final int REALTIME_DELAY_MINUTES = 20;

    private final PlaceQueryRepository placeQueryRepository;
    private final PlaceKeywordClassificationQueryRepository keywordClassificationRepository;

    public PageRes<PlaceSummaryResponseDTO> search(Long regionId, List<String> keywordCodes, int page, int size) {
        requireRegion(regionId);
        List<String> classificationCodes = resolveClassificationCodes(keywordCodes);
        if (keywordCodes != null && !keywordCodes.isEmpty() && classificationCodes.isEmpty()) {
            return new PageRes<>(List.of(), 0, page, size, false);
        }

        long totalCount = placeQueryRepository.countPlaces(regionId, classificationCodes);
        int offset = Math.max(0, (page - 1) * size);
        List<PlaceSummaryResponseDTO> items = placeQueryRepository
                .findPlaces(regionId, classificationCodes, LocalDate.now(), offset, size).stream()
                .map(row -> PlaceSummaryResponseDTO.of(
                        row.contentId(), row.title(), row.regionName(), imageOrDefault(row.imageUrl()),
                        row.congestionGrade(), row.concentrationRate()
                ))
                .toList();
        return new PageRes<>(items, totalCount, page, size, (long) offset + items.size() < totalCount);
    }

    public PlaceDetailResponseDTO getDetail(String contentId, Long regionId) {
        requireRegion(regionId);
        PlaceDetailRow row = placeQueryRepository.findPlace(contentId, LocalDate.now())
                .orElseThrow(PlaceNotFoundException::new);
        return new PlaceDetailResponseDTO(
                row.contentId(),
                row.title(),
                row.regionName(),
                textOrDefault(row.overview()),
                imageOrDefault(row.imageUrl()),
                textOrDefault(row.address()),
                row.latitude(),
                row.longitude(),
                new PlaceAccessibilityResponseDTO(
                        row.wheelchairAccessible(),
                        row.strollerAccessible(),
                        textOrDefault(row.wheelchairDescription()),
                        textOrDefault(row.strollerDescription())
                ),
                PlaceCongestionResponseDTO.of(row.forecastGrade(), row.forecastRate()),
                realtimeCongestion(row),
                getForecasts(contentId, DEFAULT_FORECAST_DAYS),
                row.dataSyncedAt(),
                DATA_SOURCE
        );
    }

    public List<PlaceForecastResponseDTO> getForecasts(String contentId, int days) {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(days - 1L);
        List<PlaceForecastRow> rows = placeQueryRepository.findForecasts(contentId, fromDate, toDate);
        return rows.stream()
                .map(row -> new PlaceForecastResponseDTO(
                        row.forecastDate(), row.concentrationRate(), row.concentrationGrade(), row.collectedAt()
                ))
                .toList();
    }

    private PlaceRealtimeCongestionResponseDTO realtimeCongestion(PlaceDetailRow row) {
        boolean delayed = row.realtimeCollectedAt() == null
                || Duration.between(row.realtimeCollectedAt(), LocalDateTime.now()).toMinutes()
                > REALTIME_DELAY_MINUTES;
        return new PlaceRealtimeCongestionResponseDTO(
                row.realtimeLevel(),
                textOrDefault(row.realtimeMessage()),
                row.populationMin(),
                row.populationMax(),
                row.observedAt(),
                row.realtimeCollectedAt(),
                delayed
        );
    }

    private void requireRegion(Long regionId) {
        if (!placeQueryRepository.regionExists(regionId)) {
            throw new RegionNotFoundException();
        }
    }

    private List<String> resolveClassificationCodes(List<String> keywordCodes) {
        if (keywordCodes == null || keywordCodes.isEmpty()) {
            return List.of();
        }
        List<String> classificationCodes = new ArrayList<>();
        for (String keywordCode : keywordCodes) {
            String keywordName = KeywordCode.toKeywordName(keywordCode);
            if (keywordName != null) {
                classificationCodes.addAll(keywordClassificationRepository
                        .findClassificationLevel2CodesByKeywordName(keywordName));
            }
        }
        return classificationCodes.stream().distinct().toList();
    }

    private String textOrDefault(String value) {
        return value == null || value.isBlank() ? NO_INFORMATION : value;
    }

    private String imageOrDefault(String value) {
        return value == null || value.isBlank() ? DEFAULT_IMAGE_URL : value;
    }
}
