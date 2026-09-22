package com.tourswitch.domain.place.provider;

import com.tourswitch.domain.place.model.PlaceCrowdForecast;
import com.tourswitch.domain.place.model.PlaceCrowdForecastQuery;
import com.tourswitch.global.client.tourapi.TatsCnctrRateClient;
import com.tourswitch.global.client.tourapi.TourApiCongestionItem;
import com.tourswitch.global.spatial.SpotNameMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TourAPI 집중률 데이터를 관광지명과 날짜 기준으로 조회해 장소 도메인에 제공한다.
 */
@Component
@RequiredArgsConstructor
public class PlaceCrowdForecastProvider {

    private static final DateTimeFormatter BASE_YMD_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int FORECAST_DAYS = 30;

    private final TatsCnctrRateClient tatsCnctrRateClient;

    /**
     * 지정한 날짜의 자치구 관광지별 집중률을 정규화한 관광지명 키로 반환한다.
     */
    public Map<String, BigDecimal> findDailyRates(String areaCode, String districtCode, LocalDate forecastDate) {
        Map<String, BigDecimal> ratesByPlaceName = new LinkedHashMap<>();
        for (TourApiCongestionItem item : fetch(areaCode, districtCode)) {
            if (forecastDate.equals(parseDate(item.baseYmd()))) {
                String placeNameKey = SpotNameMatcher.key(item.touristSpotName());
                if (!placeNameKey.isEmpty()) {
                    ratesByPlaceName.put(placeNameKey, item.concentrationRate());
                }
            }
        }
        return Map.copyOf(ratesByPlaceName);
    }

    /**
     * 관광지명과 일치하는 오늘 이후 최대 30일의 예상 집중률을 날짜순으로 반환한다.
     */
    public List<PlaceCrowdForecast> findWeeklyForecast(PlaceCrowdForecastQuery query) {
        Set<String> attractionNameKeys = attractionNameKeys(query.placeName(), query.aliasNames());
        if (attractionNameKeys.isEmpty()) {
            return List.of();
        }
        return fetch(query.areaCode(), query.districtCode()).stream()
                .filter(item -> attractionNameKeys.contains(SpotNameMatcher.key(item.touristSpotName())))
                .map(this::toForecast)
                .filter(forecast -> forecast != null && !forecast.forecastDate().isBefore(query.startDate()))
                .sorted(java.util.Comparator.comparing(PlaceCrowdForecast::forecastDate))
                .limit(FORECAST_DAYS)
                .toList();
    }

    /**
     * 자치구의 전체 집중률 예측 원본을 조회한다.
     */
    private List<TourApiCongestionItem> fetch(String areaCode, String districtCode) {
        return tatsCnctrRateClient.tatsCnctrRatedList(areaCode, districtCode);
    }

    /**
     * 기본 관광지명과 수동 검수한 별칭을 정확 매칭에 사용할 정규화 키 집합으로 변환한다.
     */
    private Set<String> attractionNameKeys(String placeName, List<String> aliasNames) {
        java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
        addNameKey(keys, placeName);
        (aliasNames == null ? List.<String>of() : aliasNames).forEach(aliasName -> addNameKey(keys, aliasName));
        return Set.copyOf(keys);
    }

    /**
     * 공백뿐인 이름은 매칭 후보에서 제외하고 유효한 정규화 키만 추가한다.
     */
    private void addNameKey(Set<String> keys, String name) {
        String key = SpotNameMatcher.key(name);
        if (!key.isEmpty()) {
            keys.add(key);
        }
    }

    /**
     * TourAPI 원본 날짜와 집중률을 장소 도메인 예측값으로 변환한다.
     */
    private PlaceCrowdForecast toForecast(TourApiCongestionItem item) {
        LocalDate forecastDate = parseDate(item.baseYmd());
        return forecastDate == null ? null : new PlaceCrowdForecast(forecastDate, item.concentrationRate());
    }

    /**
     * TourAPI의 yyyyMMdd 날짜 문자열을 변환하고 형식이 잘못된 항목은 제외한다.
     */
    private LocalDate parseDate(String baseYmd) {
        try {
            return LocalDate.parse(baseYmd, BASE_YMD_FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
