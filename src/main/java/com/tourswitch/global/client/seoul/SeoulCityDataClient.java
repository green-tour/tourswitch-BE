package com.tourswitch.global.client.seoul;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SeoulCityDataClient {

    private static final DateTimeFormatter OBSERVED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient restClient;
    private final SeoulOpenApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SeoulCityDataClient(@Qualifier("seoulOpenApiRestClient") RestClient restClient,
                               SeoulOpenApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Optional<SeoulCrowdSnapshot> getCrowd(String areaCode) {
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment(properties.serviceKey(), "json", properties.serviceName(), "1", "5", areaCode)
                .encode().build().toUri();
        String raw = restClient.get().uri(uri).retrieve().body(String.class);
        try {
            JsonNode cityData = objectMapper.readTree(raw).path("CITYDATA");
            validateResult(cityData);
            JsonNode population = cityData.path("LIVE_PPLTN_STTS");
            if (population.isArray()) population = population.path(0);
            if (population.isMissingNode() || population.isNull()) return Optional.empty();
            Integer firstPopulation = integer(population, "AREA_PPLTN_MIN");
            Integer secondPopulation = integer(population, "AREA_PPLTN_MAX");
            Integer populationMin = firstPopulation == null || secondPopulation == null
                    ? firstPopulation : Math.min(firstPopulation, secondPopulation);
            Integer populationMax = firstPopulation == null || secondPopulation == null
                    ? secondPopulation : Math.max(firstPopulation, secondPopulation);
            return Optional.of(new SeoulCrowdSnapshot(
                    text(cityData, "AREA_CD"), text(cityData, "AREA_NM"),
                    text(population, "AREA_CONGEST_LVL"), text(population, "AREA_CONGEST_MSG"),
                    populationMin, populationMax,
                    "Y".equalsIgnoreCase(text(population, "REPLACE_YN")),
                    LocalDateTime.parse(text(population, "PPLTN_TIME"), OBSERVED_AT_FORMAT)));
        } catch (Exception exception) {
            throw new IllegalStateException("서울시 실시간 도시데이터 응답을 해석하지 못했습니다.", exception);
        }
    }

    private void validateResult(JsonNode cityData) {
        String code = cityData.path("RESULT").path("RESULT.CODE").asText();
        if (code.isBlank()) code = cityData.path("RESULT").path("CODE").asText();
        if (!code.isBlank() && !"INFO-000".equals(code)) {
            throw new IllegalStateException("서울시 API 오류: " + code);
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : Integer.valueOf(value);
    }
}
