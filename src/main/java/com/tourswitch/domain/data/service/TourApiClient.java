package com.tourswitch.domain.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.global.config.ExternalApiProperties;
import com.tourswitch.global.config.ExternalHttpProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class TourApiClient {

    private static final String TOUR_BASE_URL = "https://apis.data.go.kr/B551011";
    private static final int PAGE_SIZE = 1_000;
    private static final int MAX_PAGE = 1_000;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ExternalApiProperties properties;
    private final ExternalHttpProperties httpProperties;

    public List<TouristSpotSource> fetchSeoulTouristSpots() {
        return fetchPages("/KorService2/areaBasedList2",
                builder -> builder.queryParam("lDongRegnCd", "11")).stream()
                .map(this::toTouristSpot)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<FestivalPeriodSource> fetchCurrentSeoulFestivals(LocalDate baseDate) {
        return fetchPages("/KorService2/searchFestival2", builder -> builder
                .queryParam("areaCode", "1")
                .queryParam("eventStartDate", baseDate.format(DateTimeFormatter.BASIC_ISO_DATE))).stream()
                .map(this::toFestivalPeriod)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<CrowdForecastSource> fetchCrowdForecasts(List<String> districtCodes) {
        List<CrowdForecastSource> forecasts = new ArrayList<>();
        for (String districtCode : districtCodes) {
            fetchPages("/TatsCnctrRateService/tatsCnctrRatedList", builder -> builder
                    .queryParam("areaCd", "11")
                    .queryParam("signguCd", districtCode)).stream()
                    .map(item -> toCrowdForecast(item, districtCode))
                    .flatMap(Optional::stream)
                    .forEach(forecasts::add);
        }
        return forecasts;
    }

    public List<TouristOverviewSource> fetchOverviews(List<String> contentIds) {
        return fetchDetails(contentIds, this::fetchOverview, "관광지 소개");
    }

    public List<AccessibilitySource> fetchAccessibilityDetails(List<String> contentIds) {
        if (!properties.accessibilityEnabled()) {
            return List.of();
        }
        return fetchDetails(contentIds, this::fetchAccessibility, "접근성");
    }

    private Optional<TouristOverviewSource> fetchOverview(String contentId) {
        URI uri = baseBuilder("/KorService2/detailCommon2")
                .queryParam("contentId", contentId)
                .build()
                .encode()
                .toUri();
        return firstItem(uri).map(item -> new TouristOverviewSource(contentId, text(item, "overview")));
    }

    private Optional<AccessibilitySource> fetchAccessibility(String contentId) {
        URI uri = baseBuilder("/KorWithService2/detailWithTour2")
                .queryParam("contentId", contentId)
                .build()
                .encode()
                .toUri();
        return firstItem(uri).map(item -> new AccessibilitySource(
                contentId,
                text(item, "wheelchair"),
                text(item, "stroller"),
                item.deepCopy()
        ));
    }

    private List<JsonNode> fetchPages(String path, Consumer<UriComponentsBuilder> additionalParameters) {
        List<JsonNode> allItems = new ArrayList<>();
        int pageNumber = 1;
        int totalCount;
        do {
            UriComponentsBuilder builder = baseBuilder(path)
                    .queryParam("numOfRows", PAGE_SIZE)
                    .queryParam("pageNo", pageNumber++);
            additionalParameters.accept(builder);
            JsonNode body = responseBody(read(builder.build().encode().toUri()));
            extractItems(body.path("items").path("item")).forEach(allItems::add);
            totalCount = body.path("totalCount").asInt(allItems.size());
        } while (allItems.size() < totalCount && pageNumber <= MAX_PAGE);
        return allItems;
    }

    private Optional<JsonNode> firstItem(URI uri) {
        List<JsonNode> items = extractItems(responseBody(read(uri)).path("items").path("item"));
        return items.stream().findFirst();
    }

    private <T> List<T> fetchDetails(
            List<String> contentIds,
            Function<String, Optional<T>> fetcher,
            String sourceName
    ) {
        if (contentIds.isEmpty()) {
            return List.of();
        }
        try (ExecutorService executor = Executors.newFixedThreadPool(properties.resolvedDetailConcurrency())) {
            List<Future<Optional<T>>> futures = contentIds.stream()
                    .map(contentId -> executor.submit(() -> fetcher.apply(contentId)))
                    .toList();
            List<T> results = new ArrayList<>();
            for (int index = 0; index < futures.size(); index++) {
                try {
                    futures.get(index).get().ifPresent(results::add);
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof ApiRequestLimitExceededException) {
                        futures.subList(index + 1, futures.size())
                                .forEach(future -> future.cancel(true));
                        log.warn("{} 일일 요청 한도에 도달했습니다. 이번 실행 성공분={}건", sourceName, results.size());
                        break;
                    }
                    throw exception;
                }
                if ((index + 1) % 100 == 0 || index + 1 == futures.size()) {
                    log.info("{} 수집 진행: {}/{}건", sourceName, index + 1, futures.size());
                }
            }
            return results;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(sourceName + " 수집이 중단되었습니다.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(sourceName + " 수집에 실패했습니다.", exception.getCause());
        }
    }

    private JsonNode read(URI uri) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= httpProperties.resolvedMaxAttempts(); attempt++) {
            try {
                String response = restClientBuilder.build().get().uri(uri).retrieve().body(String.class);
                if (!StringUtils.hasText(response)) {
                    throw new IllegalStateException("외부 API가 빈 응답을 반환했습니다.");
                }
                return objectMapper.readTree(response);
            } catch (HttpClientErrorException.TooManyRequests exception) {
                throw new ApiRequestLimitExceededException(exception);
            } catch (HttpClientErrorException exception) {
                throw new IllegalStateException(
                        "외부 API 호출 권한 또는 요청값을 확인해야 합니다. status=" + exception.getStatusCode(),
                        exception
                );
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt < httpProperties.resolvedMaxAttempts()) {
                    waitBeforeRetry(attempt);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("외부 API JSON 파싱에 실패했습니다.", exception);
            }
        }
        throw new IllegalStateException("외부 API 호출 재시도 횟수를 초과했습니다: " + uri, lastException);
    }

    private JsonNode responseBody(JsonNode root) {
        JsonNode response = root.path("response");
        String resultCode = response.path("header").path("resultCode").asText();
        if (!"0000".equals(resultCode)) {
            String resultMessage = response.path("header").path("resultMsg").asText();
            throw new IllegalStateException("TourAPI 응답 오류: code=" + resultCode + ", message=" + resultMessage);
        }
        return response.path("body");
    }

    private List<JsonNode> extractItems(JsonNode itemNode) {
        List<JsonNode> items = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(items::add);
        } else if (itemNode.isObject()) {
            items.add(itemNode);
        }
        return items;
    }

    private UriComponentsBuilder baseBuilder(String path) {
        requireServiceKey();
        return UriComponentsBuilder.fromUriString(TOUR_BASE_URL + path)
                .queryParam("serviceKey", properties.secretKey())
                .queryParam("MobileOS", properties.mobileOs())
                .queryParam("MobileApp", properties.mobileApp())
                .queryParam("_type", "json");
    }

    private Optional<TouristSpotSource> toTouristSpot(JsonNode item) {
        int contentTypeId = item.path("contenttypeid").asInt();
        if (!List.of(12, 14, 15, 28, 32, 38, 39).contains(contentTypeId)) {
            return Optional.empty();
        }
        String contentId = text(item, "contentid");
        String title = text(item, "title");
        BigDecimal latitude = decimal(item, "mapy");
        BigDecimal longitude = decimal(item, "mapx");
        if (!StringUtils.hasText(contentId) || !StringUtils.hasText(title)
                || latitude == null || longitude == null) {
            return Optional.empty();
        }
        return Optional.of(new TouristSpotSource(
                contentId,
                contentTypeId,
                title,
                text(item, "addr1"),
                latitude,
                longitude,
                text(item, "firstimage"),
                text(item, "lclsSystm1"),
                text(item, "lclsSystm2"),
                text(item, "lclsSystm3"),
                toSeoulDistrictCode(text(item, "lDongSignguCd"))
        ));
    }

    private Optional<FestivalPeriodSource> toFestivalPeriod(JsonNode item) {
        String contentId = text(item, "contentid");
        LocalDate startDate = date(text(item, "eventstartdate"));
        LocalDate endDate = date(text(item, "eventenddate"));
        if (!StringUtils.hasText(contentId) || startDate == null || endDate == null) {
            return Optional.empty();
        }
        return Optional.of(new FestivalPeriodSource(contentId, startDate, endDate));
    }

    private Optional<CrowdForecastSource> toCrowdForecast(JsonNode item, String requestedDistrictCode) {
        String attractionName = text(item, "tAtsNm");
        BigDecimal concentrationRate = decimal(item, "cnctrRate");
        LocalDate forecastDate = date(text(item, "baseYmd"));
        if (!StringUtils.hasText(attractionName) || concentrationRate == null || forecastDate == null) {
            return Optional.empty();
        }
        return Optional.of(new CrowdForecastSource(
                text(item, "areaCd", "11"),
                text(item, "signguCd", requestedDistrictCode),
                text(item, "signguNm"),
                attractionName,
                forecastDate,
                concentrationRate
        ));
    }

    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(httpProperties.resolvedRetryDelayMillis() * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("외부 API 재시도 대기가 중단되었습니다.", exception);
        }
    }

    private void requireServiceKey() {
        if (!StringUtils.hasText(properties.secretKey())) {
            throw new IllegalStateException("TOUR_API_SERVICE_KEY가 설정되지 않았습니다.");
        }
    }

    private String text(JsonNode node, String key) {
        return text(node, key, null);
    }

    private String text(JsonNode node, String key, String fallback) {
        String value = node.path(key).asText(null);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private BigDecimal decimal(JsonNode node, String key) {
        String value = text(node, key);
        try {
            return value == null ? null : new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate date(String value) {
        try {
            return value == null ? null : LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static class ApiRequestLimitExceededException extends RuntimeException {

        private ApiRequestLimitExceededException(HttpClientErrorException.TooManyRequests cause) {
            super(cause);
        }
    }

    private String toSeoulDistrictCode(String legalDongDistrictCode) {
        if (!StringUtils.hasText(legalDongDistrictCode)) {
            return null;
        }
        String trimmedCode = legalDongDistrictCode.trim();
        if (trimmedCode.matches("\\d{3}")) {
            return "11" + trimmedCode;
        }
        return trimmedCode;
    }

    public record TouristSpotSource(
            String contentId,
            int contentTypeId,
            String title,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String firstImageUrl,
            String classificationLevel1Code,
            String classificationLevel2Code,
            String classificationLevel3Code,
            String districtCode
    ) {
    }

    public record FestivalPeriodSource(String contentId, LocalDate startDate, LocalDate endDate) {
    }

    public record TouristOverviewSource(String contentId, String overview) {
    }

    public record AccessibilitySource(
            String contentId,
            String wheelchairDescription,
            String strollerDescription,
            JsonNode barrierFreeDetail
    ) {
    }

    public record CrowdForecastSource(
            String areaCode,
            String districtCode,
            String districtName,
            String attractionName,
            LocalDate forecastDate,
            BigDecimal concentrationRate
    ) {
    }
}
