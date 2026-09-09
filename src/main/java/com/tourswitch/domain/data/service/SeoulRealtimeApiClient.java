package com.tourswitch.domain.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.global.config.ExternalHttpProperties;
import com.tourswitch.global.config.SeoulApiProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class SeoulRealtimeApiClient {

    private static final DateTimeFormatter SEOUL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm")
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .toFormatter();

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final SeoulApiProperties properties;
    private final ExternalHttpProperties httpProperties;

    public Optional<SeoulRealtimeSource> fetch(String requestedAreaCode) {
        requireServiceKey();
        URI uri = UriComponentsBuilder.fromUriString(properties.resolvedBaseUrl())
                .pathSegment(properties.secretKey(), "json", "citydata_ppltn", "1", "5", requestedAreaCode)
                .build()
                .encode()
                .toUri();
        JsonNode root = read(uri);
        JsonNode service = root.path("SeoulRtd");
        validateResult(root.path("RESULT"), requestedAreaCode);
        validateResult(service.path("RESULT"), requestedAreaCode);

        JsonNode item = firstObject(root.path("SeoulRtd.citydata_ppltn"));
        if (item.isMissingNode()) {
            item = firstObject(service.path("row"));
        }
        if (item.isMissingNode()) {
            item = firstObject(service.path("citydata_ppltn"));
        }
        if (item.isMissingNode()) {
            return Optional.empty();
        }

        LocalDateTime observedAt = dateTime(text(item, "PPLTN_TIME"));
        if (observedAt == null) {
            throw new IllegalStateException("서울 실시간 API 관측 시각 형식이 올바르지 않습니다: " + requestedAreaCode);
        }
        List<SeoulForecastSource> forecasts = new ArrayList<>();
        for (JsonNode forecast : item.path("FCST_PPLTN")) {
            LocalDateTime forecastTime = dateTime(text(forecast, "FCST_TIME"));
            if (forecastTime != null) {
                forecasts.add(new SeoulForecastSource(
                        forecastTime,
                        text(forecast, "FCST_CONGEST_LVL"),
                        integer(forecast, "FCST_PPLTN_MIN"),
                        integer(forecast, "FCST_PPLTN_MAX")
                ));
            }
        }
        return Optional.of(new SeoulRealtimeSource(
                text(item, "AREA_CD", requestedAreaCode),
                text(item, "AREA_CONGEST_LVL"),
                text(item, "AREA_CONGEST_MSG"),
                integer(item, "AREA_PPLTN_MIN"),
                integer(item, "AREA_PPLTN_MAX"),
                decimal(item, "RESNT_PPLTN_RATE"),
                decimal(item, "NON_RESNT_PPLTN_RATE"),
                observedAt,
                forecasts
        ));
    }

    private JsonNode read(URI uri) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= httpProperties.resolvedMaxAttempts(); attempt++) {
            try {
                String response = restClientBuilder.build().get().uri(uri).retrieve().body(String.class);
                if (!StringUtils.hasText(response)) {
                    throw new IllegalStateException("서울 실시간 API가 빈 응답을 반환했습니다.");
                }
                return objectMapper.readTree(response);
            } catch (HttpClientErrorException exception) {
                throw new IllegalStateException("서울 실시간 API 요청이 거부되었습니다: " + exception.getStatusCode(),
                        exception);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt < httpProperties.resolvedMaxAttempts()) {
                    waitBeforeRetry(attempt);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("서울 실시간 API JSON 파싱에 실패했습니다.", exception);
            }
        }
        throw new IllegalStateException("서울 실시간 API 재시도 횟수를 초과했습니다: " + uri, lastException);
    }

    private void validateResult(JsonNode result, String areaCode) {
        if (!result.isObject()) {
            return;
        }
        String code = result.path("CODE").asText();
        if (StringUtils.hasText(code) && !"INFO-000".equals(code)) {
            throw new IllegalStateException("서울 실시간 API 응답 오류: areaCode=" + areaCode
                    + ", code=" + code + ", message=" + result.path("MESSAGE").asText());
        }
    }

    private JsonNode firstObject(JsonNode node) {
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0);
        }
        if (node.isObject()) {
            return node;
        }
        return node.path(0);
    }

    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(httpProperties.resolvedRetryDelayMillis() * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("서울 실시간 API 재시도 대기가 중단되었습니다.", exception);
        }
    }

    private void requireServiceKey() {
        if (!StringUtils.hasText(properties.secretKey())) {
            throw new IllegalStateException("SEOUL_OPEN_API_KEY가 설정되지 않았습니다.");
        }
    }

    private String text(JsonNode node, String key) {
        return text(node, key, null);
    }

    private String text(JsonNode node, String key, String fallback) {
        String value = node.path(key).asText(null);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Integer integer(JsonNode node, String key) {
        String value = text(node, key);
        try {
            return value == null ? null : Integer.valueOf(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal decimal(JsonNode node, String key) {
        String value = text(node, key);
        try {
            return value == null ? null : new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDateTime dateTime(String value) {
        try {
            return value == null ? null : LocalDateTime.parse(value, SEOUL_DATE_TIME_FORMATTER);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public record SeoulRealtimeSource(
            String areaCode,
            String congestionLevel,
            String congestionMessage,
            Integer populationMinimum,
            Integer populationMaximum,
            BigDecimal residentRate,
            BigDecimal nonResidentRate,
            LocalDateTime observedAt,
            List<SeoulForecastSource> forecasts
    ) {
    }

    public record SeoulForecastSource(
            LocalDateTime forecastTime,
            String congestionLevel,
            Integer populationMinimum,
            Integer populationMaximum
    ) {
    }
}
